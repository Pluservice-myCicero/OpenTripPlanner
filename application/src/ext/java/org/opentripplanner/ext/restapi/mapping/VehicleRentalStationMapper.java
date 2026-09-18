package org.opentripplanner.ext.restapi.mapping;

import java.util.List;
import java.util.Locale;
import org.opentripplanner.ext.restapi.model.ApiVehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;

public class VehicleRentalStationMapper {

  public static ApiVehicleRentalStation mapToApi(VehicleRentalPlace domain, Locale locale) {
    if (domain == null) {
      return null;
    }

    ApiVehicleRentalStation api = new ApiVehicleRentalStation();

    api.id = domain.stationId();
    api.name = domain.name().toString(locale);
    api.x = domain.longitude();
    api.y = domain.latitude();
    api.bikesAvailable = domain.vehiclesAvailable();
    api.spacesAvailable = domain.spacesAvailable();
    api.allowDropoff = domain.isAllowDropoff();
    api.isFloatingBike = domain.isFloatingVehicle();
    api.isCarStation = domain.isCarStation();
    api.networks = List.of(domain.network());
    api.realTimeData = domain.isRealTimeData();
    api.rentalUris = domain.rentalUris();

    return api;
  }
}
