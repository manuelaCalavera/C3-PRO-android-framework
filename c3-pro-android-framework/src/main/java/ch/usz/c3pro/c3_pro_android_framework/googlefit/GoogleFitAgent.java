package ch.usz.c3pro.c3_pro_android_framework.googlefit;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.Field;

import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Quantity;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import ch.usz.c3pro.c3_pro_android_framework.C3PRO;
import ch.usz.c3pro.c3_pro_android_framework.googlefit.jobs.EnterHeightDataPointJob;
import ch.usz.c3pro.c3_pro_android_framework.googlefit.jobs.EnterWeightDataPointJob;
import ch.usz.c3pro.c3_pro_android_framework.googlefit.jobs.ReadAggregateStepCountJob;
import ch.usz.c3pro.c3_pro_android_framework.googlefit.jobs.ReadHeightJob;
import ch.usz.c3pro.c3_pro_android_framework.googlefit.jobs.ReadWeightJob;
import ch.usz.c3pro.c3_pro_android_framework.googlefit.jobs.ReadWeightSummaryJob;

/**
 * C3PRO
 * <p/>
 * Created by manny Weber on 06/29/2016.
 * Copyright © 2016 University Hospital Zurich. All rights reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This agent will help to request information from and write data to the Google Fit api. Before
 * using its methods, the permissions for the operations have to be requested from the user and data
 * may have to be subscribed to in order to get the information wanted.
 * */
public class GoogleFitAgent {
    public static final String LTAG = "LC3P";
    private static GoogleApiClient apiClient;

    /**
     * Interface used to pass back Quantities read from Google Fit.
     */
    public interface QuantityReceiver {
        public void receiveQuantity(String requestID, Quantity quantity);
    }

    /**
     * Interface used to pass back multiple Quantities read from Google Fit
     */
    public interface ObservationReceiver {
        public void receiveObservation(String requestID, Observation observation);
    }

    private void GoogleFitAgent() {
    }

    public static void init(GoogleApiClient googleApiClient) {
        apiClient = googleApiClient;
    }

    /**
     * Calls back with the total number of steps taken between two dates.
     * Remember to subscribe to the step count. Add permission to the AndroidManifest.xml:
     * <uses-permission android:name="android.permission.FITNESS_ACTIVITY_READ" />
     * and add the scope to the GoogleApiClient Builder .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
     */
    public static void getAggregateStepCountBetween(Date start, Date end, String requestID, QuantityReceiver quantityReceiver) {
        ReadAggregateStepCountJob job = new ReadAggregateStepCountJob(apiClient, requestID, start, end, quantityReceiver);
        C3PRO.getJobManager().addJobInBackground(job);
    }

    /**
     * Calls back with the latest entry of the user's height. If no entry is found, a Quantity of zero
     * is returned.
     * Remember to add permission to the AndroidManifest.xml: <uses-permission android:name="android.permission.BODY_SENSORS" />
     * and add the scope to the GoogleApiClient Builder .addScope(new Scope(Scopes.FITNESS_BODY_READ))
     */
    public static void getLatestSampleOfHeight(String requestID, QuantityReceiver quantityReceiver) {
        ReadHeightJob job = new ReadHeightJob(apiClient, requestID, quantityReceiver);
        C3PRO.getJobManager().addJobInBackground(job);
    }

    /**
     * Calls back with the latest entry of the user's weight. If no entry is found, a Quantity of zero
     * is returned.
     * Remember to add permission to the AndroidManifest.xml: <uses-permission android:name="android.permission.BODY_SENSORS" />
     * and add the scope to the GoogleApiClient Builder .addScope(new Scope(Scopes.FITNESS_BODY_READ))
     */
    public static void getLatestSampleOfWeight(String requestID, QuantityReceiver quantityReceiver) {
        ReadWeightJob job = new ReadWeightJob(apiClient, requestID, quantityReceiver);
        C3PRO.getJobManager().addJobInBackground(job);
    }

    /**
     * Calls back with an observation with a component with each, maximum, average, and minimum weight
     * between the specified dates. If no entries are found, the observation will not contain any
     * components.
     * Remember to add permission to the AndroidManifest.xml: <uses-permission android:name="android.permission.BODY_SENSORS" />
     * and add the scope to the GoogleApiClient Builder .addScope(new Scope(Scopes.FITNESS_BODY_READ))
     */
    public static void getWeightSummaryBetween(Date start, Date end, String requestID, ObservationReceiver observationReceiver) {
        ReadWeightSummaryJob job = new ReadWeightSummaryJob(apiClient, requestID, start, end, observationReceiver);
        C3PRO.getJobManager().addJobInBackground(job);
    }

    /**
     * For debug: prints the DataSet to the Log Console
     * */
    private static void dumpDataSet(DataSet dataSet) {
        Log.d(LTAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = DateFormat.getTimeInstance();

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.d(LTAG, "Data point:");
            Log.d(LTAG, "\tType: " + dp.getDataType().getName());
            Log.d(LTAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.d(LTAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for (Field field : dp.getDataType().getFields()) {
                Log.d(LTAG, "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));
            }
        }
    }

    /**
     * Writes a current weight reading to google fit history. The current date and time is used for
     * the data point as start and end time.
     * Permission has to be declared in the AndroidManifest: <uses-permission android:name="android.permission.FITNESS_ACTIVITY_READ_WRITE" />
     * and Api and scope added when building the GoogleApiClient: .addApi(Fitness.HISTORY_API)
     * .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
     */
    public static void enterWeightDataPoint(Context context, float weight) {
        Date now = new Date();
        EnterWeightDataPointJob job = new EnterWeightDataPointJob(apiClient, context, now, now, weight);
        C3PRO.getJobManager().addJobInBackground(job);
    }

    /**
     * Writes a current height reading to google fit history. The current date and time is used for
     * the data point as start and end time.
     * Permission has to be declared in the AndroidManifest: <uses-permission android:name="android.permission.FITNESS_ACTIVITY_READ_WRITE" />
     * and Api and scope added when building the GoogleApiClient: .addApi(Fitness.HISTORY_API)
     * .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
     */
    public static void enterHeightDataPoint(Context context, float height) {
        Date now = new Date();
        EnterHeightDataPointJob job = new EnterHeightDataPointJob(apiClient, context, now, now, height);
        C3PRO.getJobManager().addJobInBackground(job);
    }
}