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

            //LM:android işletim servisinin konum servisine erişim sağlıyo. kullanıcının konumuyla ilgili işleri yapmaya olanak sağlar.
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                locationListener = new LocationListener() {
                //LL: bu değişiklikleri dinliyor ve method veriyor ve bu metod içinde değişen konumla ilgili işlemleri yapıyoruz.
                //yani LM'nin verdiği mesajları alıp değiştircen mi napıcan harita mı oynatcan ne istiyosan al burda yap diyo yani onLocationChanged kısmında

                @Override
                public void onLocationChanged(@NonNull Location location) {

                    //Sadece 1 defa onlocationChanged çağırma işlemi:
                    //Bir defada olsa bunu çağırıcaz o yüzden shared prefencesa kaydederiz
                    //shared preferencs içine bir bilgi kaydedicem olayı: Bir defa çalıştırıldı mı çalıştırılmadı mı?
                    //böylelikle 1 defa çalıştırmış olucaz sadece uygulama her başladığında tabi
                    info = sharedPreferences.getBoolean("info", false);
                    //info diye kayıtlı bir şey var dedim ama yoksa değeri false olsun böylece ilk defa bu çalıştırıldığında içerde böyle kayıtlı
                    //bişi olmicak sonuçta ve bu false 1 kere döndürecek.

                    if (!info) { //eğer false ise bunu çalıştırayım yani false ve çalıştıracak

                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        //LatLng objesi verebilmek için location'ı böyle Latlng çeviririz
                        //LatLng moveCamera bu ikisi kullanıcın konumuna odaklanan haritayı gösterir
                        //Yani kamerayı bulunduğumuz konuma çevirmek için bu iki satır kodu yazmamız lazım
                        //ve artık kullanıcı her değişiklik yaptığında direkt kullanıcının konumuna götürür bizi
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));

                        sharedPreferences.edit().putBoolean("info", true).apply();
                        //bu sefer true kaydederim ve bir defa çalıştıktan sonra true olmuş olacağı için onlocation ne kadar çağırılırsa çağırılsın
                        //daha çalışmayacak.

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
                //konum değişikliklerini isteme
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                // son konumu isteme kodu. uygulamada son konumumuz açılır.
                if(lastLocation != null){
                    LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15));
                }
                mMap.setMyLocationEnabled(true);
                //izin kontrolü altında kullanırız çünkü izinsiz kullanılmaz
                //bu kod bizim olduğumuz konuma mavi buton ekler
                //shared prefences kullanmadan onlocation altında hiçbir şey yapmadan sadece bunu kullanıcam. hiçbi sakıncası yok
            }



        }else{
            //else ise mainactivityden bi veri gönderilmiştir demek ve o veriyi gösterme işlemleri yaparız
            //
            mMap.clear();//öncesinde bir şey yazdıysak temizleyerek başlayabilirim
            selectedPlace = (Place) intent.getSerializableExtra("place"); //adapterden yollanan place'i alıyorum.
            //place'i başka yerlerde kullanacabileceğimiz için globalleştiriyoruz.

            //direkt haritaya gösterelim:
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
                        //normalde if yapmamıza gerek yok ama bug gibi bişi sanırım o yüzden izni aldığımdan emin olmamı istiyo ben de kontrol ediyorum
                        //if ile diyorum ki izin granted mi eşit mi ona tek seçenek var evet eşin ve izin kodunu if içinde yazıyorum.

                        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        // son konumu isteme kodu. uygulamada son konumumuz açılır.
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
        //bunun için public classta onMapReadyCallback yanına GoogleMap.OnMapLongClickListener bunu ekleyip metodu ekleyeceğiz.
        //sonra  mMap.setOnMapLongClickListener(this); onMapReady metodu altına yazıyoruz.
        //kullanıcı tıkladıktan sonra bu metod çağırılıyo ve nereye tıkladığı bize veriliyo.
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng));
        //uzun tıklandığında işaretleyici koyacak ve her tıklandığına birsürü işaretleyici olmaması için mapclear ekleriz.

        selectedLatitude = latLng.latitude;//latlng ile eşitliyoruz.
        selectedLongitude = latLng.longitude;

        binding.saveButton.setEnabled(true);//kullanıcı bir yer seçmeden save butonu dokunulmaz yapma kodu
        //sonra onmapready altında false yaparız ki kullanıcı bir şey seçmeden buton dokunulmaz olsun

    }
    public void save(View view){
        Place place = new Place(binding.placeNameText.getText().toString(),selectedLatitude,selectedLongitude);//enlem boylamı burda alamam onmaplongclick
        //içinde alabilirim. bunu da onmapde bu ikisinin seçildiğinden emin olup değişkene kaydettikten sonra alabilirim.

        //threading -> Main (UI), Default (CPU Intensive), IO (Network, Database)
        //main thread:ana işlemelerin yapıldığı yer kullanıcı arayüzüyle ilgili işlemler yapılır ve burada çok yüklü işlemler yaparsak

        //Default thread: arka planda çalışan genelde cpu yoğun işlemler arka arkaya işlem yapan ve işlemciyi yorabilecek işlemler
        //IO thread: girdi çıktı manasına gelir genelde network operasyonları yapılır yani netten veri istemek

        //placeDao.insert(place).subscribeOn(Schedulers.io()).subscribe();
        //kullanıcı arayüzünü bloklayabilir. uygulama çökebilir o yüzden room database kaydı arka planda yapılmalıdır. böyle değil
        //.subcscribeOn dediğimizde bunu hangi threadde yapacağımızı yazarız. böyle:Schedulers.io()).subscribe

        //subscribeOn:nerede bu işlemi yapayım
        //observeOn: nerede gözlemleyeyim açayım.

        //ÖZET OLARAK burada placeDao.inserti yap dedim ama bunu io.threadde yap ve main threadde gözlemle ve mapsAktiviteye subscribe olucam dedim
        //

        //kullanabilnek için globalde     private CompositeDisposable compositeDisposable = new CompositeDisposable(); yapıyoruz.
        //disposable: kullan at. Tüm işlemler hafızada yer tutar ve disposable arkaplanındaki işlemler bittikten sonra onDestroy ile silebiliriz.
        compositeDisposable.add(placeDao.insert(place) //bana içine eklemek istediğim şeyi soracak yani placeDao.insert
                .subscribeOn(Schedulers.io()) //kullanmak istediğim yer
                .observeOn(AndroidSchedulers.mainThread()) //main threade gözlemleyeceğim ayriyetten bunu koymasakta çalışacaktır.
                .subscribe(MapsActivity.this::handleResponse)//metodu çalıştır değil referans verme işlemi
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
        //daha önce yaptığım bütün kollar buradan çöpe atılır ve hafızada yer tutmaz.
    }
}