package scenario;

import org.junit.jupiter.api.Test;

public class SetupScenarioTest {

	@Test
	public void startFabric() throws Exception {
		System.err.println("Starting Fabric");
    	ScenarioSteps.startFabric(true);
	}

}
