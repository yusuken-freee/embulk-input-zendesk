package org.embulk.input.zendesk.utils;

import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.embulk.EmbulkTestRuntime;
import org.embulk.input.zendesk.ZendeskInputPlugin.PluginTask;
import org.embulk.input.zendesk.models.Target;
import org.embulk.input.zendesk.services.ZendeskSupportAPIService;
import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.Schema;
import org.embulk.spi.json.JsonParser;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampParser;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.msgpack.value.Value;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestZendeskUtil
{
    @Rule
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private static Schema schema;
    private static Column booleanColumn;
    private static Column longColumn;
    private static Column doubleColumn;
    private static Column stringColumn;
    private static Column dateColumn;
    private static Column jsonColumn;

    private static ZendeskSupportAPIService zendeskSupportAPIService;

    @BeforeClass
    public static void setUp()
    {
        zendeskSupportAPIService = mock(ZendeskSupportAPIService.class);
        PluginTask pluginTask = ZendeskTestHelper.getConfigSource("util.yml").loadConfig(PluginTask.class);
        schema = pluginTask.getColumns().toSchema();
        booleanColumn = schema.getColumn(0);
        longColumn = schema.getColumn(1);
        doubleColumn = schema.getColumn(2);
        stringColumn = schema.getColumn(3);
        dateColumn = schema.getColumn(4);
        jsonColumn = schema.getColumn(5);
    }

    @Test
    public void testIsSupportIncrementalShouldReturnTrue()
    {
        boolean result = ZendeskUtils.isSupportAPIIncremental(Target.TICKETS);
        Assert.assertTrue(result);

        result = ZendeskUtils.isSupportAPIIncremental(Target.USERS);
        Assert.assertTrue(result);

        result = ZendeskUtils.isSupportAPIIncremental(Target.ORGANIZATIONS);
        Assert.assertTrue(result);

        result = ZendeskUtils.isSupportAPIIncremental(Target.TICKET_EVENTS);
        Assert.assertTrue(result);

        result = ZendeskUtils.isSupportAPIIncremental(Target.TICKET_METRICS);
        Assert.assertTrue(result);
    }

    @Test
    public void testNumberToSplitWithHintingInTaskWithNonIncrementalTarget()
    {
        JsonNode dataJson = ZendeskTestHelper.getJsonFromFile("data/util_page.json");
        when(zendeskSupportAPIService.getData(anyString(), anyInt(), anyBoolean(), anyLong())).thenReturn(dataJson);
        int number = ZendeskUtils.numberToSplitWithHintingInTask(2101);
        assertEquals(22, number);

        number = ZendeskUtils.numberToSplitWithHintingInTask(2100);
        assertEquals(21, number);

        number = ZendeskUtils.numberToSplitWithHintingInTask(2099);
        assertEquals(21, number);
    }

    @Test
    public void testAddRecordAllRight()
    {
        String name = "allRight";
        JsonNode dataJson = ZendeskTestHelper.getJsonFromFile("data/util.json").get(name);
        PageBuilder mock = Mockito.mock(PageBuilder.class);

        Boolean boolValue = Boolean.TRUE;
        long longValue = 1;
        double doubleValue = 1;
        String stringValue = "string";
        Timestamp dateValue = TimestampParser.of("%Y-%m-%dT%H:%M:%S%z", "UTC").parse("2019-01-01T00:00:00Z");
        Value jsonValue = new JsonParser().parse("{}");

        ZendeskUtils.addRecord(dataJson, schema, mock);

        verify(mock, times(1)).setBoolean(booleanColumn, boolValue);
        verify(mock, times(1)).setLong(longColumn, longValue);
        verify(mock, times(1)).setDouble(doubleColumn, doubleValue);
        verify(mock, times(1)).setString(stringColumn, stringValue);
        verify(mock, times(1)).setTimestamp(dateColumn, dateValue);
        verify(mock, times(1)).setJson(jsonColumn, jsonValue);
    }

    @Test
    public void testAddRecordAllWrong()
    {
        String name = "allWrong";
        JsonNode dataJson = ZendeskTestHelper.getJsonFromFile("data/util.json").get(name);
        PageBuilder mock = Mockito.mock(PageBuilder.class);

        Value jsonValue = new JsonParser().parse("{}");

        ZendeskUtils.addRecord(dataJson, schema, mock);

        verify(mock, times(1)).setNull(booleanColumn);
        verify(mock, times(1)).setNull(longColumn);
        verify(mock, times(1)).setNull(doubleColumn);
        verify(mock, times(1)).setNull(stringColumn);
        verify(mock, times(1)).setNull(dateColumn);
        verify(mock, times(1)).setJson(jsonColumn, jsonValue);
    }

    @Test
    public void testAddRecordAllMissing()
    {
        String name = "allMissing";
        JsonNode dataJson = ZendeskTestHelper.getJsonFromFile("data/util.json").get(name);
        PageBuilder mock = Mockito.mock(PageBuilder.class);

        ZendeskUtils.addRecord(dataJson, schema, mock);

        verify(mock, times(6)).setNull(Mockito.any(Column.class));
    }

    @Test
    public void testConvertBase64()
    {
        String expectedResult = "YWhrc2RqZmhramFzZGhma2phaGRma2phaGRramZoYWtqZGY=";
        String encode = ZendeskUtils.convertBase64("ahksdjfhkjasdhfkjahdfkjahdkjfhakjdf");
        assertEquals(expectedResult, encode);
    }
}
