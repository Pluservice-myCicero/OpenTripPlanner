package org.opentripplanner.ext.restapi.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.opentripplanner.api.model.geometry.EncodedPolyline;

public class EncodedPolylineSerializer extends JsonSerializer<EncodedPolyline> {

  @Override
  public void serialize(EncodedPolyline value, JsonGenerator gen, SerializerProvider serializers)
    throws IOException {
    gen.writeStartObject();
    gen.writeStringField("points", value.points());
    gen.writeNumberField("length", value.length());
    gen.writeEndObject();
  }
}
