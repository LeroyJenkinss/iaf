package nl.nn.adapterframework.configuration;

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.LogUtil;

public class TestApplicationWarnings {
	private Logger log = LogUtil.getLogger(TestApplicationWarnings.class);

	private TestConfiguration configuration = null;

	@Before
	public void setUp() {
		ApplicationWarnings.removeInstance(); //Remove old instance if present
		configuration = new TestConfiguration();
	}

	@After
	public void tearDown() {
		if(configuration != null) {
			configuration.close();
		}
	}

	@Test
	public void testApplicationContextFromSpring() {
		ApplicationWarnings applWarnings = configuration.getBean("applicationWarnings", ApplicationWarnings.class);
		assertEquals(configuration, applWarnings.getApplicationContext());
	}

	@Test
	public void testApplicationContextStaticInitialized() {
		ApplicationWarnings.add(log, "test message");

		ApplicationWarnings applWarnings = configuration.getBean("applicationWarnings", ApplicationWarnings.class);

		assertEquals(configuration, applWarnings.getApplicationContext());
		assertEquals("test message", applWarnings.getWarnings().get(0));
	}

	@Test
	public void testApplicationContextFromRefreshedSpringContext() {
		ApplicationWarnings.add(log, "test message 1");

		configuration.getBean("applicationWarnings", ApplicationWarnings.class);
		configuration.refresh();

		ApplicationWarnings.add(log, "test message 2");

		ApplicationWarnings applWarnings = configuration.getBean("applicationWarnings", ApplicationWarnings.class);

		assertEquals(configuration, applWarnings.getApplicationContext());
		assertEquals("After a Context refresh it should not copy warnings over to the new instance", 1, applWarnings.getWarnings().size());
		assertEquals("test message 2", applWarnings.getWarnings().get(0));
	}
}