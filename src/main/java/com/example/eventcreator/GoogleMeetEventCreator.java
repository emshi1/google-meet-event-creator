package com.example.eventcreator;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.*;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import org.apache.http.client.utils.DateUtils;

import java.io.*;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.*;

public class GoogleMeetEventCreator {

    private static final String APPLICATION_NAME = "google-meet-event-creator";
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    /**
     * Directory to store authorization tokens for this application.
     */
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES =
        Collections.singletonList(CalendarScopes.CALENDAR_EVENTS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
        throws IOException {
        // Load client secrets.
        InputStream in = GoogleMeetEventCreator.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver)
            .authorize("google-meet-event-creator");
        //returns an authorized Credential object.
        return credential;
    }

    public static void main(String[] args) {
        String msg = args[0] + " " + args[1];
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            com.google.api.services.calendar.Calendar service =
                new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            CreateConferenceRequest createRequest = new CreateConferenceRequest()
                .setRequestId(UUID.randomUUID().toString());
            ConferenceData conferenceData = new ConferenceData()
                .setCreateRequest(createRequest);
            Date startDate = DateUtils.parseDate(msg, new String[]{"dd.mm.yyyy HH:MM"});

            Event event = new Event()
                .setAnyoneCanAddSelf(true)
                .setConferenceData(conferenceData)
                .setStart(new EventDateTime()
                    .setDateTime(new DateTime(startDate, TimeZone.getDefault())))
                .setEnd(new EventDateTime().setDateTime(new DateTime(getEndDate(startDate), TimeZone.getDefault())))
                .setGuestsCanSeeOtherGuests(true)
                .setDescription("Autogenerated");
            Event created = service.events().insert("primary", event).execute();
            System.out.println(created.getHtmlLink());

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Date getEndDate(Date startDate) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(java.util.Calendar.HOUR, 1);
        return calendar.getTime();
    }

}
