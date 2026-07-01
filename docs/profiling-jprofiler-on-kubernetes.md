# Profiling a Cloudbreak service JVM with JProfiler (Kubernetes)

How to attach JProfiler to a Cloudbreak service (core/datalake/environment/freeipa/redbeams/…) running
in a Kubernetes pod, and analyze the result.

> **The short version:** JProfiler's built-in GUI **"Attach → Kubernetes cluster"** flow **fails on our
> images** with a `NullPointerException` in `net.n3.nanoxml.IXMLElement`. The image is FIPS-hardened and
> JProfiler's helper tools can't get an `AES` cipher, so they crash and the GUI misparses the crash as XML.
> The reliable method is to **load the agent yourself with `jcmd`** and connect JProfiler to a
> port-forwarded socket. See [Method A](#method-a-manual-agent-load--remote-connect-recommended).

---

## Prerequisites

- **`kubectl` configured for the target cluster.** `kubectl config current-context` must point at the right
  cluster and namespace. If you launch JProfiler from the GUI and hit connection errors, launch it from a
  terminal that has a working `kubectl` and the correct context active.
- **JProfiler desktop app** installed locally. The agent version pushed into the pod must match your GUI;
  JProfiler handles this automatically the first time it tries to attach.

Pick your target once (adjust namespace/pod to your case):

```bash
NS=cloudbreak
POD=<your-pod>              # e.g. kubectl get pods -n $NS
CONTAINER=cloudbreak        # the app container, not the istio sidecar
```

---

## Why the normal GUI "Attach" fails (FIPS)

The container image sets, at the image level:

```
JAVA_TOOL_OPTIONS = --module-path=/usr/share/java/bouncycastle-fips
JDK_JAVA_OPTIONS  = --add-exports=... -Djavax.net.ssl.trustStoreType=FIPS
```

The main app opts back out of FIPS with `-Djava.security.properties=.../java-nonfips.security` **on its own
command line**, but any *other* Java process started in the container (including JProfiler's `install4j`-based
helper tools such as `jpenable`) does **not** get that override. Those helpers need a standard `AES` cipher
for their inter-process communication, but SunJCE has been removed, so they die with:

```
java.security.NoSuchAlgorithmException: Cannot find any provider supporting AES
```

JProfiler's Kubernetes attach runs one of these helpers right after *"Unpacking agent libraries on remote
machine"*, it crashes, and the GUI tries to parse the crash output as XML → empty element →

```
java.lang.NullPointerException: Cannot invoke
  "net.n3.nanoxml.IXMLElement.getAttribute(String, String)" because "<parameter1>" is null
```

`jcmd` (a plain JDK tool) does not use `install4j` and needs no `AES` of its own, and the JProfiler agent,
once loaded, runs **inside the app JVM** (which has working non-FIPS crypto). That's why Method A works.

---

## Method A: manual agent load + remote connect (recommended)

Gives you the full **live** JProfiler UI.

### 1. Find the app JVM's PID (inside the container)

```bash
kubectl exec -n $NS $POD -c $CONTAINER -- jcmd -l
# e.g. => "1234 /cloudbreak.jar"
PID=1234
```

### 2. Get the Linux agent `.so` into the pod

JProfiler unpacks its Linux agents into the pod on any attach attempt — even the one that NPEs. So the
easiest way to get them is to **try the GUI "Attach → Kubernetes" once and let it fail**, then find the file:

```bash
kubectl exec -n $NS $POD -c $CONTAINER -- \
  sh -c 'find / -name "libjprofilerti*.so" 2>/dev/null'
# => /root/.jprofiler16/agent/<build>_<version>/jprofiler16/bin/linux-x64/libjprofilerti.so   (glibc)
#    .../bin/linux_musl-x64/libjprofilerti.so                                                 (musl)
```

Our images are **glibc-based (Wolfi)**, so use the **`linux-x64`** variant (Alpine/musl images would use
`linux_musl-x64`). Set:

```bash
SO=/root/.jprofiler16/agent/<build>_<version>/jprofiler16/bin/linux-x64/libjprofilerti.so
```

### 3. Load the agent into the running JVM (listens on 8849, non-blocking)

```bash
kubectl exec -n $NS $POD -c $CONTAINER -- \
  jcmd $PID JVMTI.agent_load "$SO" "port=8849,nowait"
# => "return code: 0"
```

Verify it's listening on loopback inside the pod (`2291` = hex 8849, state `0A` = LISTEN):

```bash
kubectl exec -n $NS $POD -c $CONTAINER -- sh -c 'grep -i 2291 /proc/net/tcp'
# => 0100007F:2291 ... 0A ...
```

### 4. Port-forward the agent socket to your machine

```bash
kubectl port-forward -n $NS $POD 8849:8849      # keep this running
```

### 5. Connect JProfiler to it

In the JProfiler GUI, create a **New Session** that connects to an **already-running agent on a port**
(*not* Quick Attach / Kubernetes — that's the path that fails):

- **Host:** `localhost`  **Port:** `8849`
- When the *Session Startup* dialog appears, choose **CPU sampling** (low overhead) to start.

---

## Analyzing with the JProfiler MCP

The local **JProfiler MCP cannot attach to the port-forwarded agent** — its `attach` only takes a local PID
or a Docker container, and `list_jvms` only sees local JVMs. There is no host:port connect. The MCP is only
useful **after the fact**:

1. In the GUI session (Method A), record, then **Save Snapshot** to a `.jps` file.
2. Point the MCP at it: `load_snapshot` → `get_performance_hotspots` → `expand_performance_hotspot`.

(The MCP `attach` *does* work for JVMs running locally or in a local Docker container — just not for a remote
pod behind `kubectl port-forward`.)

---

## Method B: JFR without any agent (works on FIPS, lowest risk)

If you don't need JProfiler's live instrumentation, Java Flight Recorder needs nothing installed and is
production-safe. JProfiler opens `.jfr` files natively (**File → Open Snapshot**).

```bash
kubectl exec -n $NS $POD -c $CONTAINER -- \
  jcmd $PID JFR.start name=cb settings=profile maxsize=500m maxage=30m
# ...exercise the workload...
kubectl exec -n $NS $POD -c $CONTAINER -- jcmd $PID JFR.dump name=cb filename=/tmp/cb.jfr
kubectl cp $NS/$POD:/tmp/cb.jfr ./cb.jfr -c $CONTAINER
kubectl exec -n $NS $POD -c $CONTAINER -- jcmd $PID JFR.stop name=cb
```

---

## Cleanup / caveats

- **The agent cannot be unloaded.** Once loaded via `JVMTI.agent_load` it stays until the pod restarts, and
  adds overhead once you start recording. On a **shared** replica this affects everyone using it — prefer a
  dedicated/low-traffic replica, or scale a dedicated one.
- To remove it, restart the pod: `kubectl delete pod -n $NS $POD` (the ReplicaSet recreates it).
- Stop the `kubectl port-forward` when done.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| GUI attach → `NullPointerException … nanoxml.IXMLElement … is null`, right after "Unpacking agent libraries" | FIPS image: JProfiler helper can't get an `AES` provider and crashes | Use [Method A](#method-a-manual-agent-load--remote-connect-recommended) (`jcmd`) instead of GUI Attach |
| Same NPE, but *immediately* on "Kubernetes cluster → Start" (no pod tree, or wrong cluster) | wrong kubeconfig/context, or `kubectl` not usable from the GUI | Launch JProfiler from a terminal with a working `kubectl`; confirm `kubectl config current-context` |
| `jcmd` can't find the JVM | wrong container / PID | `jcmd -l` in the **app** container; the sidecar has no JVM |
| `jpenable` → `NoSuchAlgorithmException: Cannot find any provider supporting AES` | expected on FIPS | don't use `jpenable`; use `jcmd JVMTI.agent_load` (Method A) |
