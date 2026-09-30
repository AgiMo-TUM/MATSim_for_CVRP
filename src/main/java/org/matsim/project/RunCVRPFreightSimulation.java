package org.matsim.project;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlanWriter;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.controller.CarrierModule;
import org.matsim.project.businessModels.CVRP;
import org.matsim.project.converter.CVRPCarrierGenerator;
import org.matsim.project.converter.CVRPNetworkGenerator;
import org.matsim.project.converter.CVRPSolutionToTourPlan;
import org.matsim.project.converter.CVRPVehicleTypeGenerator;
import org.matsim.project.parser.JSONParser;
import org.matsim.vehicles.VehicleType;

import java.io.File;
import java.nio.file.Path;

/**
 * Main class to run a MATSim freight simulation from CVRP JSON data.
 *
 * <p>Usage:
 * <pre>
 *   // Default: use jsprit to solve the VRP
 *   RunCVRPFreightSimulation.main(new String[]{})
 *
 *   // Use the pre-planned solution from the JSON file
 *   RunCVRPFreightSimulation.main(new String[]{"--use-solution"})
 * </pre>
 */
public class RunCVRPFreightSimulation {

    private static final String SCENARIO_DIR = "scenarios/cvrp/";
    private static final String JSON_INPUT = "original-input-data/cvrp-instances/sample_30_3.json";

    public static void main(String[] args) throws Exception {
        boolean usePrePlannedSolution = false;

        if (args != null && args.length > 0) {
            usePrePlannedSolution = "--use-solution".equals(args[0]);
        }

        run(usePrePlannedSolution);
    }

    public static void run(boolean usePrePlannedSolution) throws Exception {
        // Ensure output directory exists
        new File(SCENARIO_DIR).mkdirs();

        // ===== 1. Parse CVRP instance from JSON =====
        System.out.println("=== Parsing CVRP instance ===");
        JSONParser parser = new JSONParser(Path.of(JSON_INPUT));
        CVRP cvrp = parser.getCVRPInstance();
        System.out.println("  Nodes: " + cvrp.getNodes().size());
        System.out.println("  Arcs: " + cvrp.getArcs().size());
        System.out.println("  Vehicle capacity: " + cvrp.getVehicleCapacity());
        System.out.println("  Max vehicles: " + cvrp.getNbVehicle());

        // ===== 2. Generate MATSim network =====
        System.out.println("\n=== Generating network ===");
        Network network = CVRPNetworkGenerator.generateNetwork(cvrp);
        CVRPNetworkGenerator.writeNetwork(network, SCENARIO_DIR + "network.xml");
        System.out.println("  Nodes: " + network.getNodes().size());
        System.out.println("  Links: " + network.getLinks().size());

        // ===== 3. Generate vehicle type =====
        System.out.println("\n=== Generating vehicle type ===");
        VehicleType vehicleType = CVRPVehicleTypeGenerator.createVehicleType(cvrp);
        CVRPVehicleTypeGenerator.writeVehicleTypes(vehicleType, SCENARIO_DIR + "vehicleTypes.xml");
        System.out.println("  Type: " + vehicleType.getId());
        System.out.println("  Capacity: " + cvrp.getVehicleCapacity());

        // ===== 4. Generate carrier with services =====
        System.out.println("\n=== Generating carrier ===");
        Carrier carrier = CVRPCarrierGenerator.createCarrier(cvrp, vehicleType);
        System.out.println("  Services: " + carrier.getServices().size());
        System.out.println("  Vehicles: " + carrier.getCarrierCapabilities().getCarrierVehicles().size());

        // ===== 5. Optionally apply pre-planned solution =====
        if (usePrePlannedSolution) {
            System.out.println("\n=== Applying pre-planned CVRP solution ===");
            CVRPSolutionToTourPlan.applySolution(cvrp, carrier);
        }

        // Write carrier to disk (with or without plans)
        CVRPCarrierGenerator.writeCarriers(carrier, SCENARIO_DIR + "carriers.xml");

        // ===== 6. Load MATSim config =====
        System.out.println("\n=== Loading MATSim configuration ===");
        Config config = ConfigUtils.loadConfig(SCENARIO_DIR + "config.xml", new FreightCarriersConfigGroup());
        config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

        // ===== 7. Load scenario =====
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // ===== 8. Load carriers from generated files =====
        CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

        // ===== 9. Solve VRP or use pre-planned solution =====
        if (!usePrePlannedSolution) {
            System.out.println("\n=== Running jsprit VRP solver ===");
            CarriersUtils.runJsprit(scenario);
            System.out.println("  jsprit optimization complete");

            // Write solved plans for inspection
            new CarrierPlanWriter(CarriersUtils.getCarriers(scenario))
                    .write(SCENARIO_DIR + "planned_carriers.xml");
        }

        // ===== 10. Run MATSim simulation =====
        System.out.println("\n=== Starting MATSim simulation ===");
        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new CarrierModule());
        controler.run();

        System.out.println("\n=== Simulation complete ===");
        System.out.println("  Output: " + config.controller().getOutputDirectory());
    }
}
