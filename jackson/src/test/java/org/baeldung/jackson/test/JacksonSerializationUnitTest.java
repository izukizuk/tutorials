package org.baeldung.jackson.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;

import org.baeldung.jackson.ignore.MyDto;
import org.baeldung.jackson.ignore.MyDtoFieldNameChanged;
import org.baeldung.jackson.ignore.MyDtoIgnoreField;
import org.baeldung.jackson.ignore.MyDtoIgnoreFieldByName;
import org.baeldung.jackson.ignore.MyDtoIncludeNonDefault;
import org.baeldung.jackson.ignore.MyDtoWithFilter;
import org.baeldung.jackson.ignore.MyMixInForString;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.collect.Lists;

public class JacksonSerializationUnitTest {

    // tests - single entity to json

    @Test
    public final void givenOnlyNonDefaultValuesAreSerializedAndDtoHasOnlyDefaultValues_whenSerializing_thenCorrect() throws JsonParseException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final String dtoAsString = mapper.writeValueAsString(new MyDtoIncludeNonDefault());

        assertThat(dtoAsString, not(containsString("intValue")));
        System.out.println(dtoAsString);
    }

    @Test
    public final void givenOnlyNonDefaultValuesAreSerializedAndDtoHasNonDefaultValue_whenSerializing_thenCorrect() throws JsonParseException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final MyDtoIncludeNonDefault dtoObject = new MyDtoIncludeNonDefault();
        dtoObject.setBooleanValue(true);

        final String dtoAsString = mapper.writeValueAsString(dtoObject);

        assertThat(dtoAsString, containsString("booleanValue"));
        System.out.println(dtoAsString);
    }

    @Test
    public final void givenNameOfFieldIsChanged_whenSerializing_thenCorrect() throws JsonParseException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final MyDtoFieldNameChanged dtoObject = new MyDtoFieldNameChanged();
        dtoObject.setStringValue("a");

        final String dtoAsString = mapper.writeValueAsString(dtoObject);

        assertThat(dtoAsString, not(containsString("stringValue")));
        assertThat(dtoAsString, containsString("strVal"));
        System.out.println(dtoAsString);
    }

    // ignore

    @Test
    public final void givenFieldIsIgnoredByName_whenDtoIsSerialized_thenCorrect() throws JsonParseException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final MyDtoIgnoreFieldByName dtoObject = new MyDtoIgnoreFieldByName();
        dtoObject.setBooleanValue(true);

        final String dtoAsString = mapper.writeValueAsString(dtoObject);

        assertThat(dtoAsString, not(containsString("intValue")));
        assertThat(dtoAsString, containsString("booleanValue"));
        System.out.println(dtoAsString);
    }

    @Test
    public final void givenFieldIsIgnoredDirectly_whenDtoIsSerialized_thenCorrect() throws JsonParseException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final MyDtoIgnoreField dtoObject = new MyDtoIgnoreField();

        final String dtoAsString = mapper.writeValueAsString(dtoObject);

        assertThat(dtoAsString, not(containsString("intValue")));
        assertThat(dtoAsString, containsString("booleanValue"));
        System.out.println(dtoAsString);
    }

    @Test
    public final void givenFieldTypeIsIgnored_whenDtoIsSerialized_thenCorrect() throws JsonParseException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.addMixInAnnotations(String.class, MyMixInForString.class);
        final MyDto dtoObject = new MyDto();
        dtoObject.setBooleanValue(true);

        final String dtoAsString = mapper.writeValueAsString(dtoObject);

        assertThat(dtoAsString, containsString("intValue"));
        assertThat(dtoAsString, containsString("booleanValue"));
        assertThat(dtoAsString, not(containsString("stringValue")));
        System.out.println(dtoAsString);
    }

    @Test
    public final void givenTypeHasFilterThatIgnoresFieldByName_whenDtoIsSerialized_thenCorrect() throws JsonParseException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleBeanPropertyFilter theFilter = SimpleBeanPropertyFilter.serializeAllExcept("intValue");
        final FilterProvider filters = new SimpleFilterProvider().addFilter("myFilter", theFilter);

        final MyDtoWithFilter dtoObject = new MyDtoWithFilter();
        dtoObject.setIntValue(12);

        final String dtoAsString = mapper.writer(filters).writeValueAsString(dtoObject);

        assertThat(dtoAsString, not(containsString("intValue")));
        assertThat(dtoAsString, containsString("booleanValue"));
        assertThat(dtoAsString, containsString("stringValue"));
        System.out.println(dtoAsString);
    }

    @Test
    public final void givenTypeHasFilterThatIgnoresNegativeInt_whenDtoIsSerialized_thenCorrect() throws JsonParseException, IOException {
        final PropertyFilter theFilter = new SimpleBeanPropertyFilter() {
            @Override
            public final void serializeAsField(final Object pojo, final JsonGenerator jgen, final SerializerProvider provider, final PropertyWriter writer) throws Exception {
                if (include(writer)) {
                    if (!writer.getName().equals("intValue")) {
                        writer.serializeAsField(pojo, jgen, provider);
                        return;
                    }

                    final int intValue = ((MyDtoWithFilter) pojo).getIntValue();
                    if (intValue >= 0) {
                        writer.serializeAsField(pojo, jgen, provider);
                    }
                } else if (!jgen.canOmitFields()) { // since 2.3
                    writer.serializeAsOmittedField(pojo, jgen, provider);
                }
            }

            @Override
            protected final boolean include(final BeanPropertyWriter writer) {
                return true;
            }

            @Override
            protected final boolean include(final PropertyWriter writer) {
                return true;
            }
        };
        final FilterProvider filters = new SimpleFilterProvider().addFilter("myFilter", theFilter);

        final MyDtoWithFilter dtoObject = new MyDtoWithFilter();
        dtoObject.setIntValue(-1);

        final ObjectMapper mapper = new ObjectMapper();
        final String dtoAsString = mapper.writer(filters).writeValueAsString(dtoObject);

        assertThat(dtoAsString, not(containsString("intValue")));
        assertThat(dtoAsString, containsString("booleanValue"));
        assertThat(dtoAsString, containsString("stringValue"));
        System.out.println(dtoAsString);
    }

    // tests - multiple entities to json

    @Test
    public final void whenDtoIsSerialized_thenCorrect() throws JsonParseException, IOException {
        final List<MyDto> listOfDtos = Lists.newArrayList(new MyDto("a", 1, true), new MyDto("bc", 3, false));

        final ObjectMapper mapper = new ObjectMapper();
        final String dtosAsString = mapper.writeValueAsString(listOfDtos);

        System.out.println(dtosAsString);
    }

}
