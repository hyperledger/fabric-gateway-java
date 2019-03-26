package scenario;

import org.junit.Test;

public class CleanupScenarioTest {

	@Test
	public void stopFabric() throws Exception {
		ScenarioSteps.stopFabric(true);
	}

}
