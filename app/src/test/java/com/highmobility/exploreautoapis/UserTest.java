package com.highmobility.exploreautoapis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * Created by ttiganik on 30/03/2017.
 */

public class UserTest {
    @Test
    public void initialization() throws JSONException {
        String jsonString = "{\"valid_until\":\"2017-04-28T03:33:33+00:00\",\"user_name\":\"Admin\",\"access_token\":\"eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJVc2VyOjEiLCJleHAiOjE0OTMzNTA0MTMsImlhdCI6MTQ5MDc1ODQxMywiaXNzIjoiTWVtYmVyQ2VudGVySW50ZXJmYWNlQXBwIiwianRpIjoiNTM2M2JlNGEtMDUzNS00Y2RkLWI1NTQtNTIyYTc1NzllYTY3IiwicGVtIjp7fSwic3ViIjoiVXNlcjoxIiwidHlwIjoiYWNjZXNzIn0.5QkOsox3_TfWNDKIL0e39kVihMsf1pyvuoOnC9sE0vFuulxUBvi1BCtHW4Op3AxKUF7aDvty49UQXHkxIBgbbw\"}";
        String dateFormat = "yyyy-MM-dd'T'HH:mm:ssXXX";

        Gson gson = new GsonBuilder().setDateFormat(dateFormat).create();
        User user = gson.fromJson(jsonString, User.class);

        Date validUntil = new Date(1493350413000l);

        assertEquals(user.getValidUntil().equals(validUntil), true);
        assertEquals(user.getUserName(), "Admin");
        assertEquals(user.getAccessToken(), "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJVc2VyOjEiLCJleHAiOjE0OTMzNTA0MTMsImlhdCI6MTQ5MDc1ODQxMywiaXNzIjoiTWVtYmVyQ2VudGVySW50ZXJmYWNlQXBwIiwianRpIjoiNTM2M2JlNGEtMDUzNS00Y2RkLWI1NTQtNTIyYTc1NzllYTY3IiwicGVtIjp7fSwic3ViIjoiVXNlcjoxIiwidHlwIjoiYWNjZXNzIn0.5QkOsox3_TfWNDKIL0e39kVihMsf1pyvuoOnC9sE0vFuulxUBvi1BCtHW4Op3AxKUF7aDvty49UQXHkxIBgbbw");
    }
}
