//package com.sequenceiq.cloudbreak.core.flow;
//
//import static reactor.event.selector.Selectors.$;
//
//import java.util.concurrent.ArrayBlockingQueue;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.TimeUnit;
//
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import reactor.core.Environment;
//import reactor.core.Reactor;
//import reactor.core.spec.Reactors;
//import reactor.event.Event;
//import reactor.event.dispatch.ThreadPoolExecutorDispatcher;
//import reactor.function.Consumer;
//
//public class FlowHandlerPOCTest {
//    private static final Logger LOGGER = LoggerFactory.getLogger(FlowHandlerPOCTest.class);
//
//    private Reactor reactor;
//    private DummyFlowHandler dummyFlowHandler;
//    private DummyErrorConsumer dummyErrorConsumer;
//    private static final BlockingQueue<String> results = new ArrayBlockingQueue<String>(2);
//
//    interface DummyService {
//        public String dummyCall(Event<String> event) throws Exception;
//    }
//
//    class DummyFlowHandler extends AbstractFlowHandler<String> {
//        @Override
//        protected void execute(Event<String> event) throws Exception {
//
//            String result = new DummyService() {
//                @Override
//                public String dummyCall(Event<String> event) throws Exception {
//                    String ret = null;
//                    switch (event.getData()) {
//                    case "happy":
//                        ret = "happy";
//                        break;
//                    case "error":
//                        throw new Exception();
//                    default:
//                        break;
//                    }
//                    return ret;
//                }
//            }.dummyCall(event);
//
//            results.offer(result);
//        }
//
//        @Override protected void next() {
//            LOGGER.info("next() called");
//        }
//
//        @Override protected void handleError(Throwable throwable, Object data) {
//
//        }
//    }
//
//    class DummyErrorConsumer implements Consumer<Throwable> {
//        @Override
//        public void accept(Throwable throwable) {
//            results.offer("error");
//            LOGGER.info("ErrorHandler Called");
//        }
//    }
//
//    @Before
//    public void setUp() throws Exception {
//        reactor = Reactors.reactor()
//                .env(new Environment())
//                .dispatcher(new ThreadPoolExecutorDispatcher(10, 10))
//                .get();
//        reactor.on($("test"), new DummyFlowHandler());
//        dummyErrorConsumer = new DummyErrorConsumer();
//        dummyFlowHandler = new DummyFlowHandler();
//        results.clear();
//    }
//
//    @Test
//    public void shouldDummyHandlerBeCalled() throws InterruptedException {
//        Event<String> event = Event.wrap("happy");
//        reactor.notify("test", event);
//
//        String result = results.poll(1000, TimeUnit.MILLISECONDS);
//        Assert.assertEquals("Happy flow not as expected!", "happy", result);
//    }
//
//    @Test
//    public void shoudErrorConsumerBeCalledOnException() throws InterruptedException {
//        Event<String> event = new Event<String>(null, "error", dummyErrorConsumer);
//        reactor.notify("test", event);
//
//        String result = results.poll(1000, TimeUnit.MILLISECONDS);
//        Assert.assertEquals("Error flow not as expected", "error", result);
//    }
//}