package org.opentripplanner.ext.accessibilityscore;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.WheelchairTraversalInformation;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * An experimental feature for calculating a numeric score between 0 and 1 which indicates how
 * accessible the itinerary is as a whole. This is not a very scientific method but just a rough
 * guidance that expresses certainty or uncertainty about the accessibility.
 * <p>
 * The intended audience for this score are frontend developers wanting to show a simple UI rather
 * than having to iterate over all the stops and trips.
 * <p>
 * Note: the information to calculate this score are all available to the frontend, however
 * calculating them on the backend makes life a little easier and changes are automatically applied
 * to all frontends.
 */
public class DecorateWithAccessibilityScore implements ItineraryDecorator {

  private final double wheelchairMaxSlope;

  public DecorateWithAccessibilityScore(double wheelchairMaxSlope) {
    this.wheelchairMaxSlope = wheelchairMaxSlope;
  }

  @Override
  public Itinerary decorate(Itinerary itinerary) {
    return addAccessibilityScore(itinerary);
  }

  private Itinerary addAccessibilityScore(Itinerary i) {
    var builder = i.copyOf();

    builder.transformLegs(leg -> {
      if (leg instanceof ScheduledTransitLeg transitLeg) {
        return transitLeg.copyOf().withAccessibilityScore(compute(transitLeg)).build();
      } else if (leg instanceof StreetLeg streetLeg && leg.isWalkingLeg()) {
        return streetLeg.withAccessibilityScore((float) compute(streetLeg));
      } else {
        return leg;
      }
    });
    if (i.isWalkOnly() || i.hasTransit()) {
      builder.withAccessibilityScore(compute(builder.legs()));
    }
    return builder.build();
  }

  private static float compute(ScheduledTransitLeg leg) {
    var fromStop = leg.from().stop.getWheelchairAccessibility();
    var toStop = leg.to().stop.getWheelchairAccessibility();
    var trip = leg.tripWheelchairAccessibility();

    var values = List.of(trip, fromStop, toStop);
    var sum = (float) values
      .stream()
      .mapToDouble(DecorateWithAccessibilityScore::accessibilityScore)
      .sum();
    return sum / values.size();
  }

  private static Float compute(List<Leg> legs) {
    return legs
      .stream()
      .map(Leg::accessibilityScore)
      .filter(Objects::nonNull)
      .min(Comparator.comparingDouble(Float::doubleValue))
      .orElse(null);
  }

  private static double accessibilityScore(Accessibility wheelchair) {
    return switch (wheelchair) {
      case NO_INFORMATION -> 0.5;
      case POSSIBLE -> 1;
      case NOT_POSSIBLE -> 0;
    };
  }

  private double compute(StreetLeg leg) {
    var edges = leg.listWalkSteps().stream().map(WalkStep::getEdges).flatMap(List::stream).toList();
    var streetEdges = edges
      .stream()
      .filter(StreetEdge.class::isInstance)
      .map(StreetEdge.class::cast)
      .toList();

    // PLUSERVICE: accessibility score based on the percentage of wheelchair-accessible path distance
    var accessibleMeters = streetEdges
      .stream()
      .filter(StreetEdge::isWheelchairAccessible)
      .mapToDouble(StreetEdge::getDistanceMeters)
      .sum();

    double totalMeters = streetEdges.stream().mapToDouble(StreetEdge::getDistanceMeters).sum();

    double accessibilityScore = totalMeters == 0 ? 0 : accessibleMeters / totalMeters;

    return Math.max(accessibilityScore, 0);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass())
      .addNum("wheelchairMaxSlope", wheelchairMaxSlope)
      .toString();
  }
}
