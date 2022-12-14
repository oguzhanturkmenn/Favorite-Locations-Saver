package com.example.javamaps.view;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.javamaps.Place.Place;
import com.example.javamaps.R;
import com.example.javamaps.Roomdb.PlaceDao;
import com.example.javamaps.Roomdb.PlaceDatabase;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.javamaps.databinding.ActivityMapsBinding;
import com.google.android.material.snackbar.Snackbar;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    LocationManager locationManager;
    LocationListener locationListener;
    ActivityResultLauncher<String> permissionLauncher;
    SharedPreferences sharedPreferences;
    Boolean info;
    PlaceDatabase db;
    PlaceDao placeDao;
    Double selectedLatitude;
    Double selectedLongitude;
    Place selectedPlace;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

     binding = ActivityMapsBinding.inflate(getLayoutInflater());
     setContentView(binding.getRoot());


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        registerLauncher();

        sharedPreferences = this.getSharedPreferences("com.example.javamaps",MODE_PRIVATE);
        info = false;

        db = Room.databaseBuilder(getApplicationContext(),PlaceDatabase.class,"Places").build();
        placeDao = db.placeDao();



        selectedLatitude = 0.0;
        selectedLongitude = 0.0;

        binding.saveButton.setEnabled(false);


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);

        Intent intent = getIntent();
        String intentInfo = intent.getStringExtra("info");

        if(intentInfo.equals("new")){
            binding.saveButton.setVisibility(View.VISIBLE);
            binding.deleteButton.setVisibility(View.GONE);

            //LM:android i??letim servisinin konum servisine eri??im sa??l??yo. kullan??c??n??n konumuyla ilgili i??leri yapmaya olanak sa??lar.
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                locationListener = new LocationListener() {
                //LL: bu de??i??iklikleri dinliyor ve method veriyor ve bu metod i??inde de??i??en konumla ilgili i??lemleri yap??yoruz.
                //yani LM'nin verdi??i mesajlar?? al??p de??i??tircen mi nap??can harita m?? oynatcan ne istiyosan al burda yap diyo yani onLocationChanged k??sm??nda

                @Override
                public void onLocationChanged(@NonNull Location location) {

                    //Sadece 1 defa onlocationChanged ??a????rma i??lemi:
                    //Bir defada olsa bunu ??a????r??caz o y??zden shared prefencesa kaydederiz
                    //shared preferencs i??ine bir bilgi kaydedicem olay??: Bir defa ??al????t??r??ld?? m?? ??al????t??r??lmad?? m???
                    //b??ylelikle 1 defa ??al????t??rm???? olucaz sadece uygulama her ba??lad??????nda tabi
                    info = sharedPreferences.getBoolean("info", false);
                    //info diye kay??tl?? bir ??ey var dedim ama yoksa de??eri false olsun b??ylece ilk defa bu ??al????t??r??ld??????nda i??erde b??yle kay??tl??
                    //bi??i olmicak sonu??ta ve bu false 1 kere d??nd??recek.

                    if (!info) { //e??er false ise bunu ??al????t??ray??m yani false ve ??al????t??racak

                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        //LatLng objesi verebilmek i??in location'?? b??yle Latlng ??eviririz
                        //LatLng moveCamera bu ikisi kullan??c??n konumuna odaklanan haritay?? g??sterir
                        //Yani kameray?? bulundu??umuz konuma ??evirmek i??in bu iki sat??r kodu yazmam??z laz??m
                        //ve art??k kullan??c?? her de??i??iklik yapt??????nda direkt kullan??c??n??n konumuna g??t??r??r bizi
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));

                        sharedPreferences.edit().putBoolean("info", true).apply();
                        //bu sefer true kaydederim ve bir defa ??al????t??ktan sonra true olmu?? olaca???? i??in onlocation ne kadar ??a????r??l??rsa ??a????r??ls??n
                        //daha ??al????mayacak.

                    }
                }


            };
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                    Snackbar.make(binding.getRoot(),"permission needed for maps",Snackbar.LENGTH_INDEFINITE).setAction("Give permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //requist permission
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                        }
                    }).show();
                }else{
                    //requist permission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);

                }
            }else{
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
                //konum de??i??ikliklerini isteme
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                // son konumu isteme kodu. uygulamada son konumumuz a????l??r.
                if(lastLocation != null){
                    LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15));
                }
                mMap.setMyLocationEnabled(true);
                //izin kontrol?? alt??nda kullan??r??z ????nk?? izinsiz kullan??lmaz
                //bu kod bizim oldu??umuz konuma mavi buton ekler
                //shared prefences kullanmadan onlocation alt??nda hi??bir ??ey yapmadan sadece bunu kullan??cam. hi??bi sak??ncas?? yok
            }



        }else{
            //else ise mainactivityden bi veri g??nderilmi??tir demek ve o veriyi g??sterme i??lemleri yapar??z
            //
            mMap.clear();//??ncesinde bir ??ey yazd??ysak temizleyerek ba??layabilirim
            selectedPlace = (Place) intent.getSerializableExtra("place"); //adapterden yollanan place'i al??yorum.
            //place'i ba??ka yerlerde kullanacabilece??imiz i??in globalle??tiriyoruz.

            //direkt haritaya g??sterelim:
            LatLng latLng = new LatLng(selectedPlace.latitude,selectedPlace.longitude);//
            mMap.addMarker(new MarkerOptions().position(latLng).title(selectedPlace.name));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));

            binding.placeNameText.setText(selectedPlace.name);
            binding.saveButton.setVisibility(View.GONE);
            binding.deleteButton.setVisibility(View.VISIBLE);

        }




    }
    public void registerLauncher(){
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    if ((ContextCompat.checkSelfPermission(MapsActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)){
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
                        //normalde if yapmam??za gerek yok ama bug gibi bi??i san??r??m o y??zden izni ald??????mdan emin olmam?? istiyo ben de kontrol ediyorum
                        //if ile diyorum ki izin granted mi e??it mi ona tek se??enek var evet e??in ve izin kodunu if i??inde yaz??yorum.

                        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        // son konumu isteme kodu. uygulamada son konumumuz a????l??r.
                        if(lastLocation != null){
                            LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15));
                        }
                    }
                }else{
                    Toast.makeText(MapsActivity.this, "Permission Needed", Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        //bunun i??in public classta onMapReadyCallback yan??na GoogleMap.OnMapLongClickListener bunu ekleyip metodu ekleyece??iz.
        //sonra  mMap.setOnMapLongClickListener(this); onMapReady metodu alt??na yaz??yoruz.
        //kullan??c?? t??klad??ktan sonra bu metod ??a????r??l??yo ve nereye t??klad?????? bize veriliyo.
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng));
        //uzun t??kland??????nda i??aretleyici koyacak ve her t??kland??????na birs??r?? i??aretleyici olmamas?? i??in mapclear ekleriz.

        selectedLatitude = latLng.latitude;//latlng ile e??itliyoruz.
        selectedLongitude = latLng.longitude;

        binding.saveButton.setEnabled(true);//kullan??c?? bir yer se??meden save butonu dokunulmaz yapma kodu
        //sonra onmapready alt??nda false yapar??z ki kullan??c?? bir ??ey se??meden buton dokunulmaz olsun

    }
    public void save(View view){
        Place place = new Place(binding.placeNameText.getText().toString(),selectedLatitude,selectedLongitude);//enlem boylam?? burda alamam onmaplongclick
        //i??inde alabilirim. bunu da onmapde bu ikisinin se??ildi??inden emin olup de??i??kene kaydettikten sonra alabilirim.

        //threading -> Main (UI), Default (CPU Intensive), IO (Network, Database)
        //main thread:ana i??lemelerin yap??ld?????? yer kullan??c?? aray??z??yle ilgili i??lemler yap??l??r ve burada ??ok y??kl?? i??lemler yaparsak

        //Default thread: arka planda ??al????an genelde cpu yo??un i??lemler arka arkaya i??lem yapan ve i??lemciyi yorabilecek i??lemler
        //IO thread: girdi ????kt?? manas??na gelir genelde network operasyonlar?? yap??l??r yani netten veri istemek

        //placeDao.insert(place).subscribeOn(Schedulers.io()).subscribe();
        //kullan??c?? aray??z??n?? bloklayabilir. uygulama ????kebilir o y??zden room database kayd?? arka planda yap??lmal??d??r. b??yle de??il
        //.subcscribeOn dedi??imizde bunu hangi threadde yapaca????m??z?? yazar??z. b??yle:Schedulers.io()).subscribe

        //subscribeOn:nerede bu i??lemi yapay??m
        //observeOn: nerede g??zlemleyeyim a??ay??m.

        //??ZET OLARAK burada placeDao.inserti yap dedim ama bunu io.threadde yap ve main threadde g??zlemle ve mapsAktiviteye subscribe olucam dedim
        //

        //kullanabilnek i??in globalde     private CompositeDisposable compositeDisposable = new CompositeDisposable(); yap??yoruz.
        //disposable: kullan at. T??m i??lemler haf??zada yer tutar ve disposable arkaplan??ndaki i??lemler bittikten sonra onDestroy ile silebiliriz.
        compositeDisposable.add(placeDao.insert(place) //bana i??ine eklemek istedi??im ??eyi soracak yani placeDao.insert
                .subscribeOn(Schedulers.io()) //kullanmak istedi??im yer
                .observeOn(AndroidSchedulers.mainThread()) //main threade g??zlemleyece??im ayriyetten bunu koymasakta ??al????acakt??r.
                .subscribe(MapsActivity.this::handleResponse)//metodu ??al????t??r de??il referans verme i??lemi
        );
    }
    public void handleResponse(){
        Intent intent = new Intent(MapsActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
    public void delete(View view){

        compositeDisposable.add(placeDao.delete(selectedPlace)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MapsActivity.this::handleResponse)
        );

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
        //daha ??nce yapt??????m b??t??n kollar buradan ????pe at??l??r ve haf??zada yer tutmaz.
    }
}