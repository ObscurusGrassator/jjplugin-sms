package jjplugin.obsgrass.sms;

import java.util.regex.Pattern;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.Paths;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.ContactsContract;
import android.database.Cursor;
import android.net.Uri;

import android.app.Activity;
import android.app.PendingIntent;
import android.telephony.SmsManager;



public class Functions {
    public Context context = null;
    private JSONObject data;

    Functions(Context context) { this.context = context; }

    public Result run(String methodName, JSONObject input) {
             if (methodName.equals("sendSMS")) return sendSMS(input);
        else if (methodName.equals("getNewSMSs")) return getNewSMSs(input);
        else if (methodName.equals("getContactByName")) return getContactByName(input);
        else if (methodName.equals("getContactByNumber")) return getContactByNumber(input);
        else {
            Log.e("~= jjPluginSMS", "serviceMethod " + methodName + " not exists");
            return new Result("", "serviceMethod " + methodName + " not exists");
        }
    }

    public String numberNorm(String number) {
        String result = Pattern.compile("^0+|[^0-9]", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(number).replaceAll("");
        return result.equals("") ? number : result;
    }

    public String nameNorm(String name) {
        name = Pattern.compile("[\\(\\)\\.\\[\\]]|^ +| +$", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(name).replaceAll("");
        return Pattern.compile(" +", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(name).replaceAll(" ");
    }

    public JSONObject readData() {
        File file = new File(context.getFilesDir(), "data.txt");

        if (data != null) return data;

        try {
            if (file.exists()) {
                FileReader fileReader = new FileReader(file);
                StringBuilder text = new StringBuilder();

                BufferedReader br = new BufferedReader(fileReader);
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
                br.close();

                data = new JSONObject(text.toString());
            }
            else data = new JSONObject("{\"readedIDs\":{}}");
        } catch (IOException e) {
            Log.e("~= jjPluginSMS", "readData() error: " + e.toString());
            throw new RuntimeException("readData() IOException: " + e.toString());
        } catch (JSONException e) {
            Log.e("~= jjPluginSMS", "readData() error: " + e.toString());
            throw new RuntimeException("readData() JSONException: " + e.toString());
        }

        return data;
    }

    public void writeData() {
        try {
            Files.write(
                Paths.get(context.getFilesDir() + "/data.txt"),
                data.toString().getBytes(),
                StandardOpenOption.CREATE
            );
        } catch (IOException e) {
            Log.e("~= jjPluginSMS", "writeData() error: " + e.toString());
            throw new RuntimeException("writeData(): " + e.toString());
        }
    }



    public Result getContactByNumber(JSONObject input) {
        try {
            Contact contact = getContactByNumber((String) input.get("number"));
            if (contact == null) return new Result("");
            else return new Result("{\"number\": \"" + contact.number + "\", \"fullName\": \"" + contact.fullName + "\"}");
        } catch (JSONException e) {
            Log.e("~= jjPluginSMS", "getContactByNumber() error: " + e.toString());
            return new Result("", "getContactByNumber() error: " + e.toString());
        }
    }
    public Contact getContactByNumber(Integer number) { return getContactByNumber(number.toString()); }
    public Contact getContactByNumber(String number) {
        ContentResolver cr = context.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                if (cur.getColumnIndex(ContactsContract.Contacts._ID) == -1) continue;
                if (cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME) == -1) continue;
                if (cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER) == -1) continue;
                @SuppressLint("Range") String id        = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                @SuppressLint("Range") String fullName  = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                int hasNumber = cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);

                if (cur.getInt(hasNumber) > 0) {
                    Cursor pCur = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{id},
                        null
                    );
                    while (pCur.moveToNext()) {
                        if (pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER) == -1) continue;
                        @SuppressLint("Range") String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        // Log.d("~=bbb", numberNorm(number) + " - " + numberNorm(phoneNo));
                        if (numberNorm(number).equals(numberNorm(phoneNo))) {
                            pCur.close();
                            if (cur != null) cur.close();
                            return new Contact(fullName, numberNorm(phoneNo));
                        }
                    }
                    pCur.close();
                }
            }
        }
        if (cur != null) cur.close();
        return null;
    }

    static class Result {
        public String status = null;
        public String body = null;
        public String error = null;

        Result(String body) { this.body = body; }
        Result(String body, String error) { this.error = error; }
        Result(String body, String error, String status) {
            if (body != null && !body.equals(""))
                 this.body = body;
            else this.error = error;
            this.status = status;
        }
    }

    static class Contact {
        public String fullName = null;
        public String number = null;
        public int ratio = 0;
        Contact(String fullName, String number) {
            this.fullName = fullName;
            this.number = number;
        }
        public void addRatio(int add) { ratio = ratio + add; }
    }

    public Result getContactByName(JSONObject input) {
        try {
//            return new Result("{\"number\": \"+42191571641\", \"fullName\": \"Filip Marcel\"}");
            Contact contact = getContactByName((String) input.get("name"));
            if (contact == null) return new Result("");
            else return new Result("{\"number\": \"" + contact.number + "\", \"fullName\": \"" + contact.fullName + "\"}");
        } catch (Exception e) {
            Log.e("~= jjPluginSMS", "getContactByName() error: " + e.toString());
            return new Result("", "getContactByName() error: " + e.toString());
        }
    }
    public Contact getContactByName(String name) {
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        List<String> names = Arrays.asList(nameNorm(name).split("\\s+"));
        int fullRatio = nameNorm(name).replace(" ", "").length();

        // https://www.tutorialspoint.com/how-to-get-phone-number-from-content-provider-in-android
        ContentResolver cr = context.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                if (cur.getColumnIndex(ContactsContract.Contacts._ID) == -1) continue;
                if (cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME) == -1) continue;
                if (cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER) == -1) continue;
                @SuppressLint("Range") String id        = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                @SuppressLint("Range") String fullName  = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                int hasNumber = cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);

                if (cur.getInt(hasNumber) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id},
                            null
                    );
                    while (pCur.moveToNext()) {
                        if (pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER) == -1) continue;
                        @SuppressLint("Range") String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        Contact contact = new Contact(fullName, numberNorm(phoneNo));
                        contacts.add(contact);

                        for (String name2 : names) {
                            for (int i = name2.length(); i >= 3; i--) {
                                if (Pattern.compile(" " + name2.substring(0, i) + "|^" + name2.substring(0, i)).matcher(nameNorm(fullName)).find()) {
                                    contact.addRatio(i);
                                    break;
                                }
                            }
                        }
                    }
                    pCur.close();
                }
            }
        }
        if (cur != null) cur.close();

        // https://stackoverflow.com/questions/19543862/how-can-i-sort-a-jsonarray-in-java
        Collections.sort( contacts, new Comparator<Contact>() {
            @Override
            public int compare(Contact a, Contact b) { return b.ratio - a.ratio; }
        });

        if (contacts.get(0).ratio > fullRatio * 0.7) {
            return contacts.get(0);
        } else return null;
//        return new Contact("Filip Marcel", "+42191571641");
    }



    public static String SENT = "SMS_SENT";
    public static String DELIVERED = "SMS_DELIVERED";
    public Result sendSMS(JSONObject input) {
        Result res = null;
        try {
            // https://mobiforge.com/design-development/sms-messaging-android
            // https://stackoverflow.com/questions/24673595/how-to-get-sms-sent-confirmation-for-each-contact-person-in-android/24845193#24845193
            // https://github.com/gonodono/sms-sender/tree/main

            ArrayList<PendingIntent> sentPendingIntents      = new ArrayList<PendingIntent>();
            ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();

            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> mSMSMessage = smsManager.divideMessage(input.getString("message"));

            for (int i = 0; i < mSMSMessage.size(); i++) {
                Intent iSent = new Intent(SENT)
                        .setPackage(context.getPackageName())
                        .setClass(context, SMSBroadcastReceiver.class)
                        .setFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        // .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("requestID", input.getString("requestID"))
                        .putExtra("intentFilterBroadcastString", input.getString("intentFilterBroadcastString"));
                Intent iDelivered = new Intent(DELIVERED)
                        .setPackage(context.getPackageName())
                        .setClass(context, SMSBroadcastReceiver.class)
                        .setFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        // .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("requestID", input.getString("requestID"))
                        .putExtra("intentFilterBroadcastString", input.getString("intentFilterBroadcastString"));

                PendingIntent sentPI      = PendingIntent.getBroadcast(context, i, iSent,      PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
                PendingIntent deliveredPI = PendingIntent.getBroadcast(context, i, iDelivered, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

                sentPendingIntents.add(i, sentPI);
                deliveredPendingIntents.add(i, deliveredPI);
            }

            smsManager.sendMultipartTextMessage(input.getString("number"), null, mSMSMessage, sentPendingIntents, deliveredPendingIntents);

            res = new Result("", "", JJPluginSMSService.WAIT_TO_BROADCAST_RECEIVER);
        } catch (Exception e) {
            Log.e("~= jjPluginSMS", "SMS error: " + e.toString());
            res = new Result("", "SMS error: " + e.toString());
        }

        return res;
    }

    /**
     * module: react-native-get-sms-android
     * const filter = {
     *       box: 'inbox', // 'inbox' (default), 'sent', 'draft', 'outbox', 'failed', 'queued', and '' for all
     *
     *       // minDate: 1554636310165, // timestamp (in milliseconds since UNIX epoch)
     *       // maxDate: 1556277910456, // timestamp (in milliseconds since UNIX epoch)
     *       // bodyRegex: '(.*)How are you(.*)', // content regex to match
     *
     *       read: 0, // 0 for unread SMS (default), 1 for SMS already read
     *       // _id: 1234, // specify the msg id
     *       // thread_id: 12, // specify the conversation thread_id
     *       // address: '+1888------', // sender's phone number
     *       // body: 'How are you', // content to match
     *
     *       // // the next 2 filters can be used for pagination
     *       // indexFrom: 0, // start from index 0
     *       // maxCount: 10, // count of SMS to return each time
     *   };
     */
    public Result getNewSMSs(JSONObject input) {
        try {
            JSONObject filterJ = input;
            String uri_filter = filterJ.has("box") ? filterJ.optString("box") : "inbox";
            int fread = filterJ.has("read") ? filterJ.optInt("read") : 0;
            int fid = filterJ.has("_id") ? filterJ.optInt("_id") : -1;
            int ftid = filterJ.has("thread_id") ? filterJ.optInt("thread_id") : -1;
            String faddress = filterJ.optString("address");
            String fcontent = filterJ.optString("body");
            String fContentRegex = filterJ.optString("bodyRegex");
            int indexFrom = filterJ.has("indexFrom") ? filterJ.optInt("indexFrom") : 0;
            int maxCount = filterJ.has("maxCount") ? filterJ.optInt("maxCount") : -1;
            String selection = filterJ.has("selection") ? filterJ.optString("selection") : "";
            String sortOrder = filterJ.has("sortOrder") ? filterJ.optString("sortOrder") : null;
            long maxDate = filterJ.has("maxDate") ? filterJ.optLong("maxDate") : -1;
            long minDate = filterJ.has("minDate") ? filterJ.optLong("minDate") : -1;
            Cursor cursor = context.getContentResolver().query(
                Uri.parse("content://sms/" + uri_filter), null, selection, null, sortOrder
            );
            int c = 0;
            JSONArray jsons = new JSONArray();
            JSONObject readedIDs = (JSONObject) readData().get("readedIDs");

            while (cursor != null && cursor.moveToNext()) {
                JSONObject json = getJsonFromCursor(cursor);
                if (cursor.getInt(cursor.getColumnIndex("read")) == 1 && readedIDs.has(json.get("_id").toString()))
                    readedIDs.remove(json.get("_id").toString());

                boolean matchFilter = true;
                if (fid > -1)
                    matchFilter = fid == cursor.getInt(cursor.getColumnIndex("_id"));
                else if (ftid > -1)
                    matchFilter = ftid == cursor.getInt(cursor.getColumnIndex("thread_id"));
                else if (fread > -1)
                    matchFilter = fread == cursor.getInt(cursor.getColumnIndex("read"));
                else if (faddress != null && !faddress.isEmpty())
                    matchFilter = faddress.equals(cursor.getString(cursor.getColumnIndex("address")).trim());
                else if (fcontent != null && !fcontent.isEmpty())
                    matchFilter = fcontent.equals(cursor.getString(cursor.getColumnIndex("body")).trim());

                if (fContentRegex != null && !fContentRegex.isEmpty())
                    matchFilter = matchFilter && cursor.getString(cursor.getColumnIndex("body")).matches(fContentRegex);
                if (maxDate > -1)
                    matchFilter = matchFilter && maxDate >= cursor.getLong(cursor.getColumnIndex("date"));
                if (minDate > -1)
                    matchFilter = matchFilter && minDate <= cursor.getLong(cursor.getColumnIndex("date"));
                if (matchFilter) {
                    if (c >= indexFrom && (fread != 1 && !readedIDs.has(json.get("_id").toString()))) {
                        if (maxCount > 0 && c >= indexFrom + maxCount) break;
                        // Long dateTime = Long.parseLong(cursor.getString(cursor.getColumnIndex("date")));
                        // String message = cursor.getString(cursor.getColumnIndex("body"));
                        jsons.put(json);
                        if (filterJ.has("setAsRread") && filterJ.optBoolean("setAsRread")) {
                            readedIDs.put(json.get("_id").toString(), 1);
                            writeData();
                            // already not working
                            // String SmsMessageId = cursor.getString(cursor.getColumnIndex("_id"));
                            // ContentValues values = new ContentValues();
                            // values.put("read", 1);
                            // // values.put("seen", 1);
                            // context.getContentResolver().update(Uri.parse("content://sms/inbox"), values, "_id=" + SmsMessageId, null);
                        }
                    }
                    c++;
                }
            }
            cursor.close();

            // https://stackoverflow.com/questions/19543862/how-can-i-sort-a-jsonarray-in-java
            JSONArray sorted = new JSONArray();
            ArrayList<JSONObject> jsonValues = new ArrayList<JSONObject>();

            for (int i = 0; i < jsons.length(); i++) {
                jsonValues.add(jsons.getJSONObject(i));
            }

            Collections.sort( jsonValues, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject a, JSONObject b) {
                    Long valA = null;
                    Long valB = null;

                    try {
                        valA = (Long) a.get("date");
                        valB = (Long) b.get("date");
                    }
                    catch (JSONException e) {
                        //do something
                    }

                    return valA.compareTo(valB);
                }
            });

            for (int i = 0; i < jsons.length(); i++) {
                sorted.put(jsonValues.get(i));
            }

            JSONObject result = new JSONObject();
            for (int i = 0; i < sorted.length(); i++) {
                JSONObject sms = sorted.getJSONObject(i);
                String number = numberNorm((String) sms.get("address"));

                if (result.has(number)) ((JSONArray) ((JSONObject) result.get(number)).get("messages")).put((String) sms.get("body"));
                else {
                    JSONObject newi = new JSONObject();
                    newi.put("messages", new JSONArray().put((String) sms.get("body")));
                    newi.put("timestamp", (Long) sms.get("date"));
                    newi.put("number", (String) sms.get("address"));
                    Contact contact = getContactByNumber((String) sms.get("address"));
                    if (contact != null) newi.put("fullName", contact.fullName);
                    result.put(number, newi);
                }
            }

            return new Result(result.toString());
        } catch (Exception e) {
            Log.e("~= jjPluginSMS", "getNewSMSs() error: " + e.getMessage());
            return new Result("", "getNewSMSs() error: " + e.getMessage());
        }
    }

    private JSONObject getJsonFromCursor(Cursor cur) {
        JSONObject json = new JSONObject();

        int nCol = cur.getColumnCount();
        String[] keys = cur.getColumnNames();
        try {
            for (int j = 0; j < nCol; j++)
                switch (cur.getType(j)) {
                    case Cursor.FIELD_TYPE_NULL:
                        json.put(keys[j], null);
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        json.put(keys[j], cur.getLong(j));
                        break;
                    case Cursor.FIELD_TYPE_FLOAT:
                        json.put(keys[j], cur.getFloat(j));
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        json.put(keys[j], cur.getString(j));
                        break;
                    case Cursor.FIELD_TYPE_BLOB:
                        json.put(keys[j], cur.getBlob(j));
                }
        } catch (Exception e) {
            return null;
        }

        return json;
    }
}
