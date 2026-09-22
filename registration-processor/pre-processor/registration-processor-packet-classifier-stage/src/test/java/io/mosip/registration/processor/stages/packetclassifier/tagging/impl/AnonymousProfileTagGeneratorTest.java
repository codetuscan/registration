package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.status.service.AnonymousProfileService;

/**
 * The Class AnonymousProfileTagGeneratorTest.
 *
 * The supervisor decision and comment are populated by AnonymousProfileServiceImpl,
 * not here - this generator only has to hand the service the workflowInstanceId and
 * tag whatever JSON comes back. That enrichment is covered by
 * AnonymousProfileServiceImplTest.
 */
@RefreshScope
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "com.sun.org.apache.xerces.*",
	"javax.xml.*", "org.xml.*" })
public class AnonymousProfileTagGeneratorTest {

	private static final String TAG_NAME = "anonymous";

	private static final String WORKFLOW_INSTANCE_ID = "8e34c5d5-2ba1-4d69-9e60-31b0a1d1c1d0";

	private static final String REGISTRATION_ID = "10001100010000120260824";

	private static final String PROFILE_JSON = "{\"processName\":\"NEW\",\"status\":\"PROCESSING\","
			+ "\"assisted\":[\"110024\",\"SUP001\"],\"supervisorId\":\"SUP001\","
			+ "\"supervisorDecision\":\"APPROVED\",\"supervisorComment\":\"Verified by supervisor\"}";

	@InjectMocks
	private AnonymousProfileTagGenerator anonymousProfileTagGenerator;

	@Mock
	private AnonymousProfileService anonymousProfileService;

	@Mock
	private PriorityBasedPacketManagerService priorityBasedPacketManagerService;

	@Before
	public void setup() throws Exception {
		Whitebox.setInternalState(anonymousProfileTagGenerator, "tagName", TAG_NAME);
		Mockito.when(anonymousProfileService.buildJsonStringFromPacketInfo(any(), any(), any(), any(), anyString(),
				anyString(), any())).thenReturn(PROFILE_JSON);
	}

	private Map<String, String> generateTags() throws Exception {
		return anonymousProfileTagGenerator.generateTags(WORKFLOW_INSTANCE_ID, REGISTRATION_ID, "NEW",
				new HashMap<>(), null, 0);
	}

	/** The profile the service builds is tagged verbatim. */
	@Test
	public void profileIsTaggedTest() throws Exception {
		assertEquals(PROFILE_JSON, generateTags().get(TAG_NAME));
	}

	/**
	 * The workflowInstanceId must reach the service - it is the key the supervisor
	 * decision and comment are looked up by, and without it both stay null.
	 */
	@Test
	public void workflowInstanceIdIsPassedToTheServiceTest() throws Exception {
		generateTags();

		Mockito.verify(anonymousProfileService).buildJsonStringFromPacketInfo(any(), any(), any(), any(), anyString(),
				anyString(), eq(WORKFLOW_INSTANCE_ID));
	}

	/**
	 * A build failure still leaves classification unblocked with no tag - the
	 * workflow manager then falls back to building the profile from the packet.
	 */
	@Test
	public void noTagWhenProfileBuildFailsTest() throws Exception {
		Mockito.when(anonymousProfileService.buildJsonStringFromPacketInfo(any(), any(), any(), any(), anyString(),
				anyString(), any())).thenThrow(new RuntimeException("profile build failed"));

		assertTrue(generateTags().isEmpty());
	}

	@Test
	public void getRequiredIdObjectFieldNamesTest() throws Exception {
		List<String> result = anonymousProfileTagGenerator.getRequiredIdObjectFieldNames();
		assertTrue(result.isEmpty());
	}

}
