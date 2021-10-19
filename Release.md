## Releasing new Cloudbreak versions

### Version numbers

Cloudbreak uses [Semantic Version numbers](http://semver.org):

`<normal>[-<pre release>][+<build metadata>]`

- `normal` means the normal version number that consists of `<major>.<minor>.<patch>`, e.g.: `0.5.0`. Final released versions don't have any postfix.
- `pre-release` is a postfix that signals whether the artifact was built locally, or if it is a development or a release candidate.
- `build-metadata` is only used in locally built artifacts and contains the git hash of the latest commit. 


### Example 1: Local development versions

Local development should be done on feature branches, branched from `master`. Locally built artifacts will have a version like this:

`0.5.0-fb.2.uncommitted+24d6c4e`,

where `0.5.0` is the current development version, fb means feature branch, 2 is the number of commits since the latest development version, uncommitted means that there are uncommitted changes in the local repo and the last part is the git hash of the latest commit. To build the artifact locally simply run this gradle command:

`./gradlew clean build`

### Example 2: Development versions

A development version is released after a pull request is merged into the master branch, the version number will look like this:

`0.6.0-dev.12`

The development release also creates and pushes a git tag to the repository with the same version number. If the latest tag before the build is a `dev` tag, the build number will increment the dev number like `0.6.0-dev.13`. If it was an `rc` tag, the minor version is incremented and the dev version starts from 1, like `0.7.0-dev.1`. To create a dev version (and upload it to the repository) run this gradle command on the master:

`./gradlew clean build publishBootJavaPublicationToMavenRepository release -Prelease.scope=minor -Prelease.stage=dev`

### Example 3: Release candidates

The first release candidate can be created if the dev version seems stable enough. The first release candidate should be created on a new branch from master named `rc-<major>.<minor>` (e.g.: `rc-0.6`). The version number will look like this:

`0.6.0-rc.1`

The release task also creates a git tag and pushes a git tag with the same version. If this task is completed, the latest tag on the master branch will be a `rc` so the next master build will increment the minor version. After the `rc-<major>.<minor>` branch was created, the new features can go into master, but it is possible that the first `rc` needs to be patched before a final version is released. If a pull request is merged to the `rc-<major>.<minor>` branch the next rc version (`0.6.0-rc.2`) will be released and the gradle task will tag the commit on the `rc` branch with the same version. If the latest tag on the `rc` branch is a `final release`, the patch version will be incremented and the `rc` will start from 1 like `0.6.1-rc.1`. To create a release candidate run this gradle command:

`./gradlew clean build publishBootJavaPublicationToMavenRepository release -Prelease.scope=minor -Prelease.stage=rc`

### Example 4: Final releases

When the `rc` branch is considered stable a final version is released. The final release should be created on a new branch named `release-<major.minor>` (e.g.: `release-0.6`) from the latest commit on the `rc` branch. The final version is based on the latest release candidate tag (e.g.: if the latest rc was `0.6.2-rc.2` the final version will be `0.6.2`) and the gradle task also tags the git commit with the final version. To release a final version run this gradle command:

`./gradlew clean build publishBootJavaPublicationToMavenRepository release -Prelease.scope=minor -Prelease.stage=final`
