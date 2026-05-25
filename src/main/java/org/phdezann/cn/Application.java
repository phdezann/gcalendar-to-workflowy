package org.phdezann.cn;

import org.phdezann.cn.core.AppArgs;
import org.phdezann.cn.core.BulletCache;
import org.phdezann.cn.core.ConfigReader;
import org.phdezann.cn.core.EventCreator;
import org.phdezann.cn.core.EventFormatter;
import org.phdezann.cn.core.GoogleCalendar;
import org.phdezann.cn.core.GoogleClient;
import org.phdezann.cn.core.JsonSerializer;
import org.phdezann.cn.core.SyncTokenCache;
import org.phdezann.cn.core.converter.EventConverter;
import org.phdezann.cn.core.converter.EventDateTimeConverter;
import org.phdezann.cn.core.converter.EventStatusEnumConverter;
import org.phdezann.cn.wf.core.HttpClient;
import org.phdezann.cn.wf.core.RequestProxy;
import org.phdezann.cn.wf.core.WorkflowyClient;
import org.phdezann.cn.wf.core.request.CreatePushPollOperationBuilder;
import org.phdezann.cn.wf.core.request.EditPushPollOperationBuilder;

import com.beust.jcommander.JCommander;

public class Application {

    public static void main(String[] args) {
        AppArgs appArgs = new AppArgs();
        JCommander.newBuilder().addObject(appArgs).build().parse(args);

        var jsonDeserializer = new JsonSerializer();
        var configReader = new ConfigReader(appArgs, jsonDeserializer);
        var config = configReader.read();

        var googleClient = new GoogleClient(appArgs);
        var syncTokenCache = new SyncTokenCache(appArgs, jsonDeserializer);
        var googleCalendarClient = new GoogleCalendar(config, googleClient, syncTokenCache);
        var bulletCache = new BulletCache(appArgs, jsonDeserializer);
        var eventFormatter = new EventFormatter(config);
        var eventStatusEnumConverter = new EventStatusEnumConverter();
        var eventDateTimeConverter = new EventDateTimeConverter();
        var eventConverter = new EventConverter(eventStatusEnumConverter, eventDateTimeConverter);
        var httpClient = new HttpClient();

        var createPushPollDataBuilder = new CreatePushPollOperationBuilder(jsonDeserializer);
        var editPushPollDataBuilder = new EditPushPollOperationBuilder();
        var requestProxy = new RequestProxy(httpClient, jsonDeserializer, createPushPollDataBuilder,
                editPushPollDataBuilder);
        var workflowyClient = new WorkflowyClient(config, requestProxy);
        var eventCreator = new EventCreator(appArgs, config, googleCalendarClient,
                eventStatusEnumConverter, eventConverter, eventFormatter, workflowyClient, bulletCache,
                jsonDeserializer);

        if (appArgs.isInitTokens()) {
            googleClient.renewTokens();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(eventCreator::shutdown));
        eventCreator.startPolling();
    }

}
