package org.opentripplanner.ext.restapi.mapping;

import org.opentripplanner.core.model.accessibility.Accessibility;

public class WheelchairAccessibilityMapper {

  public static Integer mapToApi(Accessibility domain) {
    if (domain == null) {
      return 0;
    }

    return switch (domain) {
      case NO_INFORMATION -> 0;
      case POSSIBLE -> 1;
      case NOT_POSSIBLE -> 2;
    };
  }
}
