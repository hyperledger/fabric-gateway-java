package scenario;

import org.junit.jupiter.api.Test;

public class CleanupScenarioTest {

	@Test
	public void stopFabric() throws Exception {
		ScenarioSteps.stopFabric(true);
	}

}
