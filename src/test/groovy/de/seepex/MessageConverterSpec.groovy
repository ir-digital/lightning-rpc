package de.seepex

import com.fasterxml.jackson.databind.ObjectMapper
import de.seepex.domain.DeviceDataTest
import de.seepex.util.SeepexMessageConverter
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import spock.lang.Specification

class MessageConverterSpec extends Specification {

    SeepexMessageConverter messageConverter

    String sampleDeviceData = "{\n" +
            "    \"deviceId\": \"c4395990-c121-438d-b4d3-8c6894b317c2\",\n" +
            "    \"value\": -0.1566774434220436,\n" +
            "    \"sensorId\": \"111d111e-bbf0-48d2-a63b-ca4b9d36a26c\",\n" +
            "    \"formulaId\": null,\n" +
            "    \"source\": \"API\",\n" +
            "    \"username\": null,\n" +
            "    \"timeInNanoSeconds\": 1615795954309000000,\n" +
            "    \"ttl\": null,\n" +
            "    \"sensorIdAsString\": \"111d111e-bbf0-48d2-a63b-ca4b9d36a26c\",\n" +
            "    \"timeAsDate\": 1615795954309\n" +
            "}"

    def setup() {
        messageConverter = new SeepexMessageConverter(new ObjectMapper());
    }

    def "test simple json conversion"() {
        given:
        DeviceDataTest data = new DeviceDataTest()
        data.setDeviceId(UUID.fromString("c4395990-c121-438d-b4d3-8c6894b317c2"))
        data.setValue(-0.1566774434220436)
        data.setFormulaId(null)
        data.setUsername(null)
        data.setTimeInNanoSeconds(1615795954309000000)

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON)

        when:
        def message = messageConverter.toMessage(data, messageProperties)
        def convertedFromConverterMessage = messageConverter.fromMessage(message)

        and:
        Message fromMessage = new Message(sampleDeviceData.getBytes("UTF-8"), messageProperties);
        def convertedFromJsonString = messageConverter.fromMessage(fromMessage)

        then:
        DeviceDataTest dd1 = (DeviceDataTest) convertedFromConverterMessage;
        assert dd1.getDeviceId() == UUID.fromString("c4395990-c121-438d-b4d3-8c6894b317c2")
        assert convertedFromConverterMessage instanceof DeviceDataTest

        DeviceDataTest dd2 = (DeviceDataTest) convertedFromJsonString;
        assert dd2.getDeviceId() == UUID.fromString("c4395990-c121-438d-b4d3-8c6894b317c2")
        assert convertedFromJsonString instanceof DeviceDataTest
    }


}
