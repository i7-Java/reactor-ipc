/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.aeron.processor;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.IdentityProcessorVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import reactor.aeron.Context;
import reactor.aeron.support.AeronTestUtils;
import reactor.aeron.support.EmbeddedMediaDriverManager;
import reactor.core.subscriber.test.TestSubscriber;
import reactor.io.buffer.Buffer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Anatoly Kadyshev
 */
@org.testng.annotations.Test
public abstract class AeronProcessorCommonVerificationTest extends IdentityProcessorVerification<Buffer> {

	private final List<AeronProcessor> processors = new ArrayList<>();

	int streamId = 1;

	public AeronProcessorCommonVerificationTest() {
		super(new TestEnvironment(1100, true), 1100);
	}

	@BeforeClass
	public void doSetup() {
		AeronTestUtils.setAeronEnvProps();
	}

	@AfterMethod
	public void cleanUp(Method method) throws InterruptedException {
		// A previous test didn't call onComplete on the processor, manual clean up
		EmbeddedMediaDriverManager driverManager = EmbeddedMediaDriverManager.getInstance();
		if (!driverManager.isTerminated()) {

			Thread.sleep(1000);

			if (!driverManager.isTerminated()) {
				System.err.println("Possibly method " + method.getName() + " didn't call onComplete on processor");

				for (AeronProcessor processor: processors) {
					processor.shutdown();
					TestSubscriber.waitFor(5, "processor didn't terminate", processor::isTerminated);
				}

				AeronTestUtils.awaitMediaDriverIsTerminated(10);
			}
		}

		processors.clear();
	}

	@Override
	public Processor<Buffer, Buffer> createIdentityProcessor(int bufferSize) {
		AeronProcessor processor = AeronProcessor.create(createContext(streamId += 10));
		processors.add(processor);
		return processor;
	}

	abstract protected Context createContext(int streamId);

	@Override
	public Publisher<Buffer> createFailedPublisher() {
		return s -> {
			s.onSubscribe(new Subscription() {
				@Override
				public void request(long n) {
				}

				@Override
				public void cancel() {
				}
			});
			s.onError(new Exception("test"));
		};
	}

	@Override
	public ExecutorService publisherExecutorService() {
		return Executors.newCachedThreadPool();
	}

	@Override
	public Buffer createElement(int element) {
		return Buffer.wrap("" + element);
	}

	// Disabled due to Exception comparison by equals
	@Test(enabled = false)
	@Override
	public void mustImmediatelyPassOnOnErrorEventsReceivedFromItsUpstreamToItsDownstream() throws Exception {
		super.mustImmediatelyPassOnOnErrorEventsReceivedFromItsUpstreamToItsDownstream();
	}

	// Disabled due to Exception comparison by equals
	@Test(enabled = false)
	@Override
	public void required_spec104_mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError()
			throws Throwable {
		super.required_spec104_mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError();
	}

	// Disabled due to Exception comparison by equals
	@Test(enabled = false)
	@Override
	public void required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall()
			throws Throwable {
		super.required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall();
	}

	// Disabled due to Exception comparison by equals
	@Test(enabled = false)
	@Override
	public void required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall()
			throws Throwable {
		super.required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall();
	}

	// Disabled because AeronPublisher doesn't support multiple subscribers for the moment
	@Test(enabled = false)
	@Override
	public void required_mustRequestFromUpstreamForElementsThatHaveBeenRequestedLongAgo() throws Throwable {
		super.required_mustRequestFromUpstreamForElementsThatHaveBeenRequestedLongAgo();
	}

	// Disabled due to a RingBufferProcessor problem when it doesn't send Complete
	// once a Publisher completed but no subscribers were attached to the processor
	@Test(enabled = false)
	@Override
	public void required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall() throws Throwable {
		super.required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall();
	}

}