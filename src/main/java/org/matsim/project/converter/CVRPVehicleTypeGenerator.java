package org.matsim.project.converter;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.project.businessModels.CVRP;

/**
 * Creates MATSim {@link VehicleType}s from CVRP vehicle parameters.
 */
public class CVRPVehicleTypeGenerator {

    public static final String VEHICLE_TYPE_ID = "cvrp_truck";

    /**
     * Creates a vehicle type with capacity from the CVRP instance.
     *
     * @param cvrp the CVRP instance containing vehicle capacity
     * @return configured MATSim VehicleType
     */
    public static VehicleType createVehicleType(CVRP cvrp) {
        VehicleType vehicleType = VehicleUtils.createVehicleType(
                Id.create(VEHICLE_TYPE_ID, VehicleType.class));

        // Set freight capacity (uses "other" capacity for goods)
        vehicleType.getCapacity().setOther(cvrp.getVehicleCapacity());

        // Physical properties
        vehicleType.setMaximumVelocity(8.33); // ~30 km/h
        vehicleType.setLength(7.5); // typical truck length in meters
        vehicleType.setNetworkMode("car"); // use the regular road network

        // Cost information for jsprit VRP solver
        vehicleType.getCostInformation()
                .setFixedCost(100.0)       // fixed cost per vehicle used
                .setCostsPerMeter(0.001)    // distance-based cost
                .setCostsPerSecond(0.01);   // time-based cost

        return vehicleType;
    }

    /**
     * Writes vehicle types to an XML file for MATSim freight configuration.
     */
    public static void writeVehicleTypes(VehicleType vehicleType, String path) {
        Vehicles vehicles = VehicleUtils.createVehiclesContainer();
        vehicles.addVehicleType(vehicleType);
        new MatsimVehicleWriter(vehicles).writeFile(path);
    }
}
