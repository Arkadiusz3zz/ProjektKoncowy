package com.example.damyrade

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlin.math.abs

data class UserRecord(val name: String = "", val record: Int = 0)

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var navController: NavHostController
    private lateinit var database: DatabaseReference

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            handleSignInResult(account)
        } catch (e: ApiException) {
            Toast.makeText(this, "Zalogowanie nie powiodło się", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            navController = rememberNavController()
            AplikacjaNawigacyjna(navController)
        }

        // Inicjalizacja Firebase Auth
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        // Pobranie referencji do bazy danych Firebase
        database = FirebaseDatabase.getInstance("https://projektandroid-8167c-default-rtdb.europe-west1.firebasedatabase.app").reference

        // Konfiguracja logowania za pomocą Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    fun getDatabase(): DatabaseReference {
        return database
    }

    fun getCurrentUser() = auth.currentUser

    // Zapis rekordu użytkownika w bazie danych Firebase
    fun saveRecord(userName: String, record: Float) {
        val userRecord = mapOf("name" to userName, "record" to record.toInt())
        database.child("users").child(auth.currentUser!!.uid).setValue(userRecord)
            .addOnSuccessListener {
                Log.d("Firebase", "Zapis rekordu powiódł się")
            }
            .addOnFailureListener { e ->
                Log.w("Firebase", "Zapis rekordu nie powiódł się", e)
            }
    }

    // Logowanie użytkownika
    fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    // Wylogowanie użytkownika
    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener(this) {
            Toast.makeText(this, "Wylogowano", Toast.LENGTH_SHORT).show()
        }
    }

    // Obsługa wyniku logowania
    private fun handleSignInResult(account: GoogleSignInAccount?) {
        account?.let {
            val credential = GoogleAuthProvider.getCredential(it.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        auth.currentUser
                        navController.navigate("menuGlowne")
                    } else {
                        Toast.makeText(this, "Autoryzacja się nie powiodła", Toast.LENGTH_SHORT).show()
                    }
                }
        } ?: run {
            Toast.makeText(this, "Logowanie się nie powiodło", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun AplikacjaNawigacyjna(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "menuGlowne") {
        composable("menuGlowne") {
            EkranMenuGlowne(navController)
        }
        composable("ekranGry") {
            EkranGry()
        }
        composable("tabelaRekordow") {
            EkranTabelaRekordow()
        }
    }
}

@Composable
fun EkranMenuGlowne(navController: NavController) {
    val activity = LocalContext.current as MainActivity
    val currentUser = activity.getCurrentUser()
    val userName = currentUser?.displayName ?: "Gość"
    val isUserLoggedIn = remember { mutableStateOf(currentUser != null) }
    val buttonText = if (isUserLoggedIn.value) "Wyloguj" else "Zaloguj"

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Gracz: $userName",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { navController.navigate("ekranGry") },
                modifier = Modifier.width(200.dp).padding(8.dp)
            ) {
                Text(text = "Graj")
            }
            Button(
                onClick = { navController.navigate("tabelaRekordow") },
                modifier = Modifier.width(200.dp).padding(8.dp)
            ) {
                Text(text = "Tabela rekordów")
            }
            Button(
                onClick = {
                    if (isUserLoggedIn.value) {
                        activity.signOut()
                        isUserLoggedIn.value = false
                    } else {
                        activity.signIn()
                        isUserLoggedIn.value = true
                    }
                },
                modifier = Modifier.width(200.dp).padding(8.dp)
            ) {
                Text(text = buttonText)
            }
        }
    }
}

@Composable
fun EkranTabelaRekordow() {
    val activity = LocalContext.current as MainActivity
    val database = activity.getDatabase()
    var userRecords by remember { mutableStateOf(listOf<UserRecord>()) }

    LaunchedEffect(Unit) {
        database.child("users").get().addOnSuccessListener { dataSnapshot ->
            val records = dataSnapshot.children.mapNotNull { it.getValue(UserRecord::class.java) }
            userRecords = records
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column {
                userRecords.forEach { record ->
                    Text(
                        text = "${record.name} Rekord: ${record.record}",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun EkranGry() {
    val kontekst = LocalContext.current
    val activity = kontekst as MainActivity
    val menedzerSensorow = kontekst.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val akcelerometr = menedzerSensorow.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var pozycjaPilkiX by remember { mutableFloatStateOf(175f) }
    var pozycjaPilkiY by remember { mutableFloatStateOf(-50.dp.value) }
    var predkoscPilkiY by remember { mutableFloatStateOf(0f) }
    var czyNaZiemi by remember { mutableStateOf(true) }
    var czyNaPlatformie1 by remember { mutableStateOf(false) }
    var czyNaPlatformie2 by remember { mutableStateOf(false) }
    var czyMozeSkoczyc by remember { mutableStateOf(true) }
    var rekord by remember { mutableFloatStateOf(0f) }
    val zakresKorutyny = rememberCoroutineScope()
    val prawaKrawedzEkranu = pobierzPrawaKrawedzEkranuWDP().value

    // Wartosci dla pierwszej platformy (niebieskiej)
    val prawaKrawedzPlatformy1 = 170f
    val lewaKrawedzPlatformy1 = 0f
    val wysokoscPlatformy1 = 10.dp.value
    val pozycjaYPlatformy1 = -160.dp.value

    // Wartosci dla drugiej platformy (zielonej)
    val szerokoscPlatformy2 = 150.dp.value
    val wysokoscPlatformy2 = 10.dp.value
    val pozycjaYPlatformy2 = -300.dp.value
    val lewaKrawedzPlatformy2 = prawaKrawedzEkranu - szerokoscPlatformy2

    val sluchaczSensorow = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val x = it.values[0]
                pozycjaPilkiX = (pozycjaPilkiX - x * 2).coerceIn(0f, prawaKrawedzEkranu - 50.dp.value)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    DisposableEffect(Unit) {
        menedzerSensorow.registerListener(sluchaczSensorow, akcelerometr, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            menedzerSensorow.unregisterListener(sluchaczSensorow)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { _ ->
                predkoscPilkiY += 0.45f
                pozycjaPilkiY += predkoscPilkiY

                if (abs(pozycjaPilkiY) > rekord) {
                    rekord = abs(pozycjaPilkiY)
                }

                val dolnaKrawedzPilki = pozycjaPilkiY + 50.dp.value
                val gornaKrawedzPilki = pozycjaPilkiY
                val lewaKrawedzPilki = pozycjaPilkiX
                val prawaKrawedzPilki = pozycjaPilkiX + 50.dp.value

                val czyNadPlatforma1X = prawaKrawedzPilki >= lewaKrawedzPlatformy1 && lewaKrawedzPilki <= prawaKrawedzPlatformy1

                if (czyNadPlatforma1X) {
                    if (dolnaKrawedzPilki >= pozycjaYPlatformy1 && gornaKrawedzPilki <= pozycjaYPlatformy1 + wysokoscPlatformy1) {
                        if (predkoscPilkiY > 0 && gornaKrawedzPilki <= pozycjaYPlatformy1) {
                            pozycjaPilkiY = pozycjaYPlatformy1 - 50.dp.value
                            predkoscPilkiY = 0f
                            czyNaPlatformie1 = true
                            czyNaZiemi = false
                            czyMozeSkoczyc = true
                        } else if (predkoscPilkiY < 0 && dolnaKrawedzPilki >= pozycjaYPlatformy1 + wysokoscPlatformy1) {
                            predkoscPilkiY = -predkoscPilkiY * 0.5f
                        }
                    }
                } else {
                    czyNaPlatformie1 = false
                }

                val czyNadPlatforma2X =
                    prawaKrawedzPilki >= lewaKrawedzPlatformy2 && lewaKrawedzPilki <= prawaKrawedzEkranu

                if (czyNadPlatforma2X) {
                    if (dolnaKrawedzPilki >= pozycjaYPlatformy2 && gornaKrawedzPilki <= pozycjaYPlatformy2 + wysokoscPlatformy2) {
                        if (predkoscPilkiY > 0 && gornaKrawedzPilki <= pozycjaYPlatformy2) {
                            pozycjaPilkiY = pozycjaYPlatformy2 - 50.dp.value
                            predkoscPilkiY = 0f
                            czyNaPlatformie2 = true
                            czyNaZiemi = false
                            czyMozeSkoczyc = true
                        } else if (predkoscPilkiY < 0 && dolnaKrawedzPilki >= pozycjaYPlatformy2 + wysokoscPlatformy2) {
                            predkoscPilkiY = -predkoscPilkiY * 0.5f
                        }
                    }
                } else {
                    czyNaPlatformie2 = false
                }

                if (pozycjaPilkiY > -50.dp.value) {
                    pozycjaPilkiY = -50.dp.value
                    predkoscPilkiY = 0f
                    czyNaZiemi = true
                    czyMozeSkoczyc = true
                } else {
                    czyNaZiemi = false
                }

                if (czyNaPlatformie1) {
                    if (pozycjaPilkiX < lewaKrawedzPlatformy1 || pozycjaPilkiX + 50.dp.value > prawaKrawedzPlatformy1) {
                        czyNaPlatformie1 = false
                    }
                }
                if (czyNaPlatformie2) {
                    if (pozycjaPilkiX < lewaKrawedzPlatformy2 || pozycjaPilkiX + 50.dp.value > prawaKrawedzEkranu) {
                        czyNaPlatformie2 = false
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                if ((czyNaZiemi || czyNaPlatformie1 || czyNaPlatformie2) && czyMozeSkoczyc) {
                    zakresKorutyny.launch {
                        predkoscPilkiY = -15f
                        czyMozeSkoczyc = false
                    }
                }
            }
            .background(Color.LightGray),
        contentAlignment = Alignment.BottomStart
    ) {
        // Podloga
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .align(Alignment.BottomCenter),
            color = Color.DarkGray
        ) {}

        // Pierwsza platforma (niebieska)
        Surface(
            modifier = Modifier
                .width(170.dp)
                .height(10.dp)
                .offset(y = (-200).dp),
            color = Color.Blue
        ) {}

        // Druga platforma (zielona)
        Surface(
            modifier = Modifier
                .width(150.dp)
                .height(10.dp)
                .offset(x = (prawaKrawedzEkranu - szerokoscPlatformy2).dp, y = (-350).dp),
            color = Color.Green
        ) {}

        // Pilka
        Surface(
            modifier = Modifier
                .offset(pozycjaPilkiX.dp, pozycjaPilkiY.dp)
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.Red),
            color = Color.Red
        ) {}

        // Pole tekstowe z rekordem
        Text(
            text = "Rekord: ${rekord.toInt()}",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 50.dp)
        )
    }

    // Zapisz rekord do bazy danych Firebase kiedy rekord zostanie pobity
    LaunchedEffect(rekord) {
        val currentUser = activity.getCurrentUser()
        currentUser?.let {
            activity.saveRecord(it.displayName ?: "Unknown", rekord)
        }
    }
}

@Composable
fun pobierzPrawaKrawedzEkranuWDP(): Dp {
    val konfiguracja = LocalConfiguration.current
    return konfiguracja.screenWidthDp.dp
}