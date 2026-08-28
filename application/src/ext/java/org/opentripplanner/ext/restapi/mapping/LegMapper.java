package org.opentripplanner.ext.restapi.mapping;

import static org.opentripplanner.ext.restapi.mapping.ElevationMapper.mapElevation;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import org.opentripplanner.api.model.geometry.EncodedPolyline;
import org.opentripplanner.apis.support.mapping.StreetNoteMapper;
import org.opentripplanner.core.model.i18n.I18NStringMapper;
import org.opentripplanner.ext.restapi.model.ApiAlert;
import org.opentripplanner.ext.restapi.model.ApiLeg;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.model.plan.leg.StreetLeg;

public class LegMapper {

  private final WalkStepMapper walkStepMapper;
  private final StreetNoteMapper streetNoteMaperMapper;
  private final AlertMapper alertMapper;
  private final PlaceMapper placeMapper;
  private final boolean addIntermediateStops;

  private final I18NStringMapper i18NStringMapper;

  public LegMapper(Locale locale, boolean addIntermediateStops) {
    this.walkStepMapper = new WalkStepMapper(locale);
    this.streetNoteMaperMapper = new StreetNoteMapper(locale);
    this.alertMapper = new AlertMapper(locale);
    this.placeMapper = new PlaceMapper(locale);
    this.addIntermediateStops = addIntermediateStops;
    this.i18NStringMapper = new I18NStringMapper(locale);
  }

  public List<ApiLeg> mapLegs(List<Leg> domain) {
    if (domain == null) {
      return null;
    }

    List<ApiLeg> apiLegs = new ArrayList<>();

    final int size = domain.size();
    final int lastIdx = size - 1;

    for (int i = 0; i < size; ++i) {
      ZonedDateTime arrivalTimeFromPlace = (i == 0) ? null : domain.get(i - 1).endTime();
      ZonedDateTime departureTimeToPlace = (i == lastIdx) ? null : domain.get(i + 1).startTime();

      apiLegs.add(mapLeg(domain.get(i), arrivalTimeFromPlace, departureTimeToPlace));
    }
    return apiLegs;
  }

  public ApiLeg mapLeg(
    Leg domain,
    ZonedDateTime arrivalTimeFromPlace,
    ZonedDateTime departureTimeToPlace
  ) {
    if (domain == null) {
      return null;
    }
    ApiLeg api = new ApiLeg();
    api.startTime = GregorianCalendar.from(domain.startTime());
    api.endTime = GregorianCalendar.from(domain.endTime());

    // Set the arrival and departure times, even if this is redundant information
    api.from = placeMapper.mapPlace(
      domain.from(),
      arrivalTimeFromPlace,
      domain.startTime(),
      domain.boardStopPosInPattern(),
      domain.boardingGtfsStopSequence()
    );
    api.to = placeMapper.mapPlace(
      domain.to(),
      domain.endTime(),
      departureTimeToPlace,
      domain.alightStopPosInPattern(),
      domain.alightGtfsStopSequence()
    );

    api.departureDelay = domain.departureDelay();
    api.arrivalDelay = domain.arrivalDelay();
    api.realTime = domain.isRealTimeUpdated();
    api.isNonExactFrequency = domain.isNonExactFrequency();
    api.headway = domain.headway();
    api.distance = round3Decimals(domain.distanceMeters());
    api.generalizedCost = domain.generalizedCost();
    api.agencyTimeZoneOffset = domain.agencyTimeZoneOffset();

    if (domain instanceof TransitLeg trLeg) {
      api.transitLeg = true;
      var agency = domain.agency();
      api.agencyId = FeedScopedIdMapper.mapToApi(agency.getId());
      api.agencyName = agency.getName();
      api.agencyUrl = agency.getUrl();
      api.mode = ModeMapper.mapToApi(trLeg.mode());

      var route = domain.route();
      api.route = i18NStringMapper.mapToApi(route.getLongName());
      api.routeColor = route.getColor();
      api.routeType = domain.routeType();
      api.routeId = FeedScopedIdMapper.mapToApi(route.getId());
      api.routeShortName = route.getShortName();
      api.routeLongName = i18NStringMapper.mapToApi(route.getLongName());
      api.routeTextColor = route.getTextColor();

      var trip = domain.trip();
      api.tripId = FeedScopedIdMapper.mapToApi(trip.getId());
      api.tripShortName = trip.getShortName();
      api.tripBlockId = trip.getGtfsBlockId();
    } else if (domain instanceof StreetLeg streetLeg) {
      api.transitLeg = false;
      api.mode = ModeMapper.mapToApi(streetLeg.getMode());

      // TODO OTP2 - This should be set to the street name according to the JavaDoc
      api.route = "";
    }

    api.interlineWithPreviousLeg = domain.isInterlinedWithPreviousLeg();
    api.headsign = i18NStringMapper.mapToApi(domain.headsign());
    api.serviceDate = LocalDateMapper.mapToApi(domain.serviceDate());
    api.routeBrandingUrl = domain.routeBrandingUrl();
    if (addIntermediateStops) {
      api.intermediateStops = placeMapper.mapStopArrivals(domain.listIntermediateStops());
    }
    api.legGeometry = EncodedPolyline.of(domain.legGeometry());
    api.legElevation = mapElevation(domain.elevationProfile());
    api.steps = walkStepMapper.mapWalkSteps(domain.listWalkSteps());
    api.alerts = concatenateAlerts(
      streetNoteMaperMapper.mapToApi(domain.listStreetNotes()),
      alertMapper.mapToApi(domain.listTransitAlerts())
    );
    api.boardRule = getBoardAlightMessage(domain.boardRule());
    api.alightRule = getBoardAlightMessage(domain.alightRule());

    api.pickupBookingInfo = BookingInfoMapper.mapBookingInfoForPickup(domain.pickupBookingInfo());
    api.dropOffBookingInfo = BookingInfoMapper.mapBookingInfoForDropOff(
      domain.dropOffBookingInfo()
    );

    api.rentedBike = domain.rentedVehicle();
    api.walkingBike = domain.walkingBike();
    api.accessibilityScore = domain.accessibilityScore();

    return api;
  }

  private Double round3Decimals(double value) {
    return Math.round(value * 1000d) / 1000d;
  }

  private static String getBoardAlightMessage(PickDrop boardAlightType) {
    if (boardAlightType == null) {
      return null;
    }
    switch (boardAlightType) {
      case NONE:
        return "impossible";
      case CALL_AGENCY:
        return "mustPhone";
      case COORDINATE_WITH_DRIVER:
        return "coordinateWithDriver";
      default:
        return null;
    }
  }

  private static List<ApiAlert> concatenateAlerts(List<ApiAlert> a, List<ApiAlert> b) {
    if (a == null) {
      return b;
    } else if (b == null) {
      return a;
    } else {
      List<ApiAlert> ret = new ArrayList<>(a);
      ret.addAll(b);
      return ret;
    }
  }
}
