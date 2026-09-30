package org.matsim.project.converter;

import org.matsim.api.core.v01.Id;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlanWriter;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.CarrierVehicle;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.project.businessModels.CVRP;
import org.matsim.project.businessModels.CVRPNode;

/**
 * Creates a MATSim freight {@link Carrier} from CVRP instance data.
 *
 * <p>Each CVRP customer node (demand &gt; 0) becomes a {@link CarrierService}.
 * Vehicles are stationed at the depot (CVRP node 0).
 */
public class CVRPCarrierGenerator {

    public static final String CARRIER_ID = "cvrp_carrier";
    private static final double SERVICE_DURATION = 300.0; // 5 minutes per stop

    /**
     * Creates a carrier with services for all customers and vehicles at the depot.
     *
     * @param cvrp        the CVRP instance
     * @param vehicleType the vehicle type to use
     * @return fully configured Carrier (without tour plans – those are added by jsprit or CVRPSolutionToTourPlan)
     */
    public static Carrier createCarrier(CVRP cvrp, VehicleType vehicleType) {
        Carrier carrier = CarriersUtils.createCarrier(Id.create(CARRIER_ID, Carrier.class));

        // Create a service for each customer node (demand > 0)
        for (CVRPNode node : cvrp.getNodes()) {
            if (node.getDemand() > 0) {
                CarrierService service = CarrierService.Builder.newInstance(
                        Id.create("service_" + node.getNodeID(), CarrierService.class),
                        CVRPNetworkGenerator.getServiceLinkId(node.getNodeID())
                )
                        .setCapacityDemand(node.getDemand())
                        .setServiceDuration(SERVICE_DURATION)
                        .build();

                CarriersUtils.addService(carrier, service);
            }
        }

        // Create vehicles at the depot – limit to a reasonable number
        int nbVehicles = Math.min(cvrp.getNbVehicle(), 10);
        for (int i = 0; i < nbVehicles; i++) {
            CarrierVehicle vehicle = CarrierVehicle.Builder.newInstance(
                    Id.create("vehicle_" + i, Vehicle.class),
                    CVRPNetworkGenerator.getDepotLinkId(),
                    vehicleType
            )
                    .setEarliestStart(0)
                    .setLatestEnd(86400) // 24 hours
                    .build();

            CarriersUtils.addCarrierVehicle(carrier, vehicle);
        }

        return carrier;
    }

    /**
     * Writes a carrier (with or without tour plans) to an XML file.
     */
    public static void writeCarriers(Carrier carrier, String path) {
        Carriers carriers = new Carriers();
        carriers.addCarrier(carrier);
        new CarrierPlanWriter(carriers).write(path);
    }
}
