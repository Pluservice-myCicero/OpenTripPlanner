package org.opentripplanner.ext.restapi.model;

import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;

public record ApiFareComponent(
  FeedScopedId fareId,
  String name,
  ApiMoney price,
  List<FeedScopedId> routes
) {}
