package com.example.diariopersonal.ui.home;

import android.annotation.SuppressLint;
import android.app.Application;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.diariopersonal.ui.Incidencia;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends AndroidViewModel {
    private final Application app;
    // almacena la direccion actual obtenida a partir de la ubicacion
    private static final MutableLiveData<String> currentAddress = new MutableLiveData<>();
    // indica si se debe comprobar el permiso de ubicacion
    private final MutableLiveData<String> checkPermission = new MutableLiveData<>();
    // contiene el texto del boton de seguimiento de la ubicacion
    private final MutableLiveData<String> buttonText = new MutableLiveData<>();
    // controla la visibilidad del progressbar
    private final MutableLiveData<Boolean> progressBar = new MutableLiveData<>();

    // probando marcar mapa
    // almacena una marca en el mapa de la nueva incidencia
    private MutableLiveData<Incidencia> newIncidencia = new MutableLiveData<>();

    // actualiza la incidencia
    public void setNewIncidencia(Incidencia incidencia) {
        newIncidencia.setValue(incidencia);
    }

    // permite observar la incidencia
    public LiveData<Incidencia> getNewIncidencia() {
        return newIncidencia;
    }

    // indica si se esta siguiendo la ubicacion
    private boolean mTrackingLocation;
    // cliente de google play para obtener la ubicacion
    FusedLocationProviderClient mFusedLocationClient;
    // almacena el usuario autenticado en firebase
    private MutableLiveData<FirebaseUser> user = new MutableLiveData<>();
    //almacena las cordenadas actuales
    private final MutableLiveData<LatLng> currentLatLng = new MutableLiveData<>();

    public MutableLiveData<LatLng> getCurrentLatLng() {
        return currentLatLng;
    }

    public HomeViewModel(@NonNull Application application) {
        super(application);

        this.app = application;
    }

    public void setFusedLocationClient(FusedLocationProviderClient mFusedLocationClient) {
        this.mFusedLocationClient = mFusedLocationClient;
    }

    public static LiveData<String> getCurrentAddress() {
        return currentAddress;
    }

    public MutableLiveData<String> getButtonText() {
        return buttonText;
    }

    public MutableLiveData<Boolean> getProgressBar() {
        return progressBar;
    }

    public LiveData<String> getCheckPermission() {
        return checkPermission;
    }

    // se ejecuta cuando se actualiza la ubicacion
    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult != null) {
                // convierte las coordenadas en direccion
                fetchAddress(locationResult.getLastLocation());
            }
        }
    };

    // metodo que define como se obtinene la ubicacion
    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000); // 10 segundos
        locationRequest.setFastestInterval(5000); // 5 segundos
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    // si el seguimiento esta descativado lo inicia, si esta activado lo detiene
    public void switchTrackingLocation() {
        if (!mTrackingLocation) {
            startTrackingLocation(true);
        } else {
            stopTrackingLocation();
        }

    }

    @SuppressLint("MissingPermission")
    public void startTrackingLocation(boolean needsChecking) {
        // si needsChecking es true se comprueba el permiso de ubicacion
        if (needsChecking) {
            checkPermission.postValue("check");
        } else {
            // si no, se inicia el seguimiento de la ubicacion
            mFusedLocationClient.requestLocationUpdates(
                    getLocationRequest(),
                    mLocationCallback, null
            );

            currentAddress.postValue("Cargando...");

            // modifica el estado de la barra de progreso y el texto del boton
            progressBar.postValue(true);
            mTrackingLocation = true;
            buttonText.setValue("Detener el seguimiento de la ubicación");
        }
    }


    // metodo para detener el seguimiento de la ubicacion
    private void stopTrackingLocation() {
        if (mTrackingLocation) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            mTrackingLocation = false;
            progressBar.postValue(false);
            buttonText.setValue("Empezar a seguir la ubicación");
        }
    }

    // utiliza geocoder para convertir las cordenadas en una direccion
    private void fetchAddress(Location location) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // se ejecuta en el hilo principal
        Handler handler = new Handler(Looper.getMainLooper());

        //
        Geocoder geocoder = new Geocoder(app.getApplicationContext(), Locale.getDefault());

        // se ejecuta en un hilo en segundo plano
        executor.execute(() -> {
            List<Address> addresses = null;
            String resultMessage = "";

            try {
                // convierte las coordenadas en una lista de direcciones
                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),
                        1);

                // objeto con las coordenadas actuales
                // permite que el observe() reciba la nueva ubicacion
                LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
                currentLatLng.postValue(latlng);


                if (addresses == null || addresses.size() == 0) {
                    if (resultMessage.isEmpty()) {
                        resultMessage = "No s'ha trobat cap adreça";
                        Log.e("HomeViewModel", resultMessage);
                    }
                } else {
                    // si se ha encontrado una direccion se coge la primera y almacena la direccion
                    Address address = addresses.get(0);
                    ArrayList<String> addressParts = new ArrayList<>();

                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        addressParts.add(address.getAddressLine(i));
                    }

                    resultMessage = TextUtils.join("\n", addressParts);
                    String finalResultMessage = resultMessage;
                    handler.post(() -> {
                        if (mTrackingLocation)
                            currentAddress.postValue(String.format("Dirección: %1$s \n Hora: %2$tr", finalResultMessage, System.currentTimeMillis()));
                    });
                }

            } catch (IOException ioException) {
                resultMessage = "Servicio no disponible";
                Log.e("ERROR: fetchAdress()", resultMessage, ioException);
            } catch (IllegalArgumentException illegalArgumentException) {
                resultMessage = "Coordenadas inválidas";
                Log.e("ERROR: fetchAdress()", resultMessage + ". " + "Latitude = " + location.getLatitude() + ", Longitude = " + location.getLongitude(), illegalArgumentException);
            }
        });
    }

    public LiveData<FirebaseUser> getUser() {
        return user;
    }

    public void setUser(FirebaseUser passedUser) {
        user.postValue(passedUser);
    }

}