package nl.fietsweer.app.domain

import nl.fietsweer.app.data.Language
import java.util.Locale

open class Strings {

    open val locale: Locale = Locale.ENGLISH

    open val appName = "Fietsweer"
    open val tagline = "Which jacket do you need today?"
    open val back = "Back"
    open val next = "Next"
    open val done = "Done"
    open val cancel = "Cancel"
    open val save = "Save"
    open val delete = "Delete"
    open val edit = "Edit"
    open val add = "Add"
    open val refresh = "Refresh"
    open val retry = "Try again"
    open val close = "Close"
    open val home = "Home"
    open val work = "Work"
    open val loading = "Loading…"

    open val tabToday = "Today"
    open val tabForecast = "Forecast"
    open val tabAlerts = "Alerts"
    open val tabSettings = "Settings"

    open val welcomeTitle = "Fietsweer"
    open val welcomeBody =
        "Set your home and work once. After that this app tells you every " +
            "morning whether you need a rain jacket, a warm jacket, or both."
    open val welcomeStart = "Get started"
    open val stepOf = { step: Int, total: Int -> "Step $step of $total" }

    open val setHomeTitle = "Where do you live?"
    open val setHomeBody = "Search for a place or drag the map to your front door."
    open val setWorkTitle = "Where do you ride to?"
    open val setWorkBody = "Work, school, the station — wherever your daily ride ends."
    open val setTimesTitle = "When do you ride?"
    open val setTimesBody =
        "The forecast is scored for exactly these departure times. You can " +
            "change them any time."
    open val setNotifyTitle = "When should we tell you?"
    open val setNotifyBody =
        "You will get one notification with the verdict for both rides. " +
            "Add as many moments as you like later on."
    open val notificationsAllowed = "Notifications are on"
    open val finishGo = "Open Fietsweer"

    open val searchPlace = "Search for a place"
    open val searchNoResults = "Nothing found"
    open val locationDenied = "Location permission was denied"
    open val locationUnavailable = "Could not get a location fix"
    open val confirmLocation = "Use this spot"
    open val dragMapHint = "Drag the map to place the pin"

    open val toWork = "To work"
    open val toHome = "Back home"
    open val adviceRain = "Take a rain jacket"
    open val adviceMaybeRain = "A rain jacket might be wise"
    open val adviceVest = "Fleece weather"
    open val adviceWinter = "Winter coat weather"
    open val adviceRainVest = "Rain jacket and a fleece"
    open val adviceRainWinter = "Winter coat and a rain jacket"
    open val adviceMaybeRainVest = "Fleece, maybe a rain jacket"
    open val adviceMaybeRainWinter = "Winter coat, maybe a rain jacket"
    open val adviceNone = "Short sleeves"
    open val adviceNoneSub = "Dry and mild on both rides"

    open val chipRainJacket = "Rain jacket"
    open val chipVest = "Fleece"
    open val chipWinter = "Winter coat"
    open val chipGloves = "Gloves"
    open val chipHat = "Hat or buff"
    open val chipWindy = "Strong wind"
    open val chipFrost = "Watch for ice"
    open val chipHot = "Water bottle"
    open val chipHeavy = "Heavy shower"
    open val chipDark = "Lights on"

    open val rightNow = "Right now"
    open val feelsLike = "feels like"
    open val onTheBike = "on the bike"
    open val chanceOfRain = "chance of rain"
    open val chanceOfRainShort = "rain"
    open val expectedRain = "Expected rain"
    open val wind = "Wind"
    open val headwind = "headwind"
    open val tailwind = "tailwind"
    open val crosswind = "crosswind"
    open val arrival = "Arrival"
    open val next24h = "Next 24 hours"
    open val temperature = "Temperature"
    open val precipitation = "Rain"
    open val bestMomentTitle = "Best moment to leave"
    open val bestMomentSub = "Within the slack around your departure time"
    open val bestMomentNoSlots = "No forecast for this window yet"
    open val onPlannedTime = "right on your departure time"
    open val minutesEarlier = { minutes: Int -> "$minutes min earlier" }
    open val minutesLater = { minutes: Int -> "$minutes min later" }
    open val lastUpdated = { time: String -> "Updated at $time" }
    open val updating = "Updating…"
    open val staleNotice = { time: String -> "Could not update \u2014 this is the forecast from $time" }
    open val updateFailedTitle = "Could not reach the weather service"
    open val updateFailedBody = "Check your connection and try again."
    open val noModelsTitle = "No usable forecast"
    open val noModelsBody = "The weather models returned nothing for this route."
    open val setupNeededTitle = "Set your route first"
    open val setupNeededBody = "Pick a home and a work location to get started."

    open val departureTimeline = "Departure times, next 24 hours"
    open val dryWindows = "Dry windows"
    open val noDryWindows = "No uninterrupted dry window in the next 24 hours."
    open val modelMatrix = "What does each model say?"
    open val modelMatrixSub = "Row = weather service, column = departure time"
    open val sources = "Sources"
    open val dailyOutlook = "The next few days"
    open val detailsFor = { time: String -> "Details for leaving at $time" }
    open val verdict = "Verdict"
    open val modelsSeeingRain = "Models seeing rain"
    open val agreement = "Agreement"
    open val agreeStrong = "models agree"
    open val agreeSome = "reasonable agreement"
    open val agreeSplit = "split — uncertain"
    open val ensembleChance = "Ensemble probability"
    open val outOfRange = "beyond range"
    open val radarLabel = "Rain radar"
    open val radarNoEcho = "no echo on the route"
    open val radarBeyond = "further ahead than 2 hours"
    open val radarRain = { mmPerHour: String -> "rain on the route, up to $mmPerHour mm/h" }
    open val expectedOnTheWay = "Rain expected on the way"
    open val avgMaxMm = { average: String, wettest: String -> "average $average mm, wettest model $wettest mm" }
    open val windAndFeel = "Wind and felt temperature"
    open val unknown = "unknown"
    open val legendDry = "dry"
    open val legendMostlyDry = "nearly dry"
    open val legendUncertain = "could go either way"
    open val legendLikelyWet = "likely wet"
    open val legendWet = "wet"
    open val legendNight = "faded blocks are night"
    open val legendModelDry = "this model stays dry"
    open val legendModelWet = "this model predicts rain"
    open val combined = "Combined (incl. radar)"
    open val slackRoom = { duration: String -> "$duration of slack" }

    open val riskDry = "Dry"
    open val riskAlmostDry = "Almost certainly dry"
    open val riskEither = "Could go either way"
    open val riskLikelyWet = "Good chance of getting wet"
    open val riskWet = "You will get wet"

    open val alertsTitle = "Alerts"
    open val alertsEmpty = "No notifications yet"
    open val alertsEmptyBody = "Add a moment and we will tell you what to bring."
    open val newAlert = "New notification"
    open val editAlert = "Edit notification"
    open val alertLabel = "Name"
    open val alertLabelHint = "Morning check"
    open val alertTime = "Time"
    open val alertDays = "Repeat"
    open val alertCoverage = "Reports on"
    open val coverageOutbound = "Ride to work"
    open val coverageReturn = "Ride home"
    open val coverageBoth = "Both rides"
    open val onlyWhenNeeded = "Only notify when a jacket is needed"
    open val onlyWhenNeededShort = "Only when needed"
    open val onlyWhenNeededBody = "Stay silent on dry, mild days."
    open val testNotification = "Send a test notification"
    open val everyDay = "Every day"
    open val weekdays = "Weekdays"
    open val weekend = "Weekend"
    open val neverRepeats = "Never — pick at least one day"
    open val nextFire = { time: String -> "Next: $time" }
    open val permNotifications = "Notifications are blocked"
    open val permNotificationsBody = "Android will not show alerts until you allow them."
    open val permExact = "Exact alarms are off"
    open val permExactBody = "Without them a notification can be a few minutes late."
    open val permBattery = "Battery optimisation is on"
    open val permBatteryBody = "Android may delay notifications to save power."
    open val grant = "Fix this"
    open val deleteAlertConfirm = "Delete this notification?"

    open val settingsRoute = "Route"
    open val settingsTimes = "Ride times"
    open val settingsRiding = "Riding"
    open val settingsAdvice = "Advice"
    open val settingsLook = "Appearance"
    open val settingsWidget = "Widget"
    open val widgetAdd = "Add the widget"
    open val widgetSizeNormal = "Normal, 4 \u00d7 2"
    open val widgetSizeSlim = "Slim, 4 \u00d7 1"
    open val settingsAbout = "About"
    open val outboundTime = "Leave home at"
    open val returnTime = "Leave work at"
    open val settingsFlex = "Slack around departure"
    open val flexEarlier = "Can leave earlier"
    open val flexLater = "Can leave later"
    open val flexNone = "not at all"
    open val cyclingSpeed = "Pace in still air"
    open val windAdjust = "Let the wind change the ride time"
    open val paceOnRoad = "Pace on the road"
    open val stillAirShort = "still air"
    open val rideTimeIs = { distance: String, minutes: Int -> "$distance km · $minutes min in still air" }
    open val whatIsWet = "What counts as wet?"
    open val wetEveryDrop = "Every drop"
    open val wetDrizzle = "Drizzle is fine"
    open val wetShower = "Real shower"
    open val rainJacketFrom = "Rain jacket from"
    open val rainJacketFromValue = { percent: Int -> "$percent% chance of rain" }
    open val vestBelowLabel = "Fleece below"
    open val winterBelowLabel = "Winter coat below"
    open val feltOnBike = { temperature: String -> "$temperature felt on the bike" }
    open val useRadarTitle = "Use the rain radar"
    open val theme = "Theme"
    open val themeSystem = "System"
    open val themeLight = "Light"
    open val themeDark = "Dark"
    open val dynamicColour = "Match my wallpaper"
    open val language = "Language"
    open val langSystem = "System"
    open val langNl = "Nederlands"
    open val langEn = "English"
    open val mapStyleTitle = "Map style"
    open val mapAuto = "Follow theme"
    open val mapLight = "Light"
    open val mapDark = "Dark"
    open val mapSoft = "Soft"
    open val aboutBody =
        "Fietsweer blends every weather model Open-Meteo serves for your route, " +
            "two ensemble systems and the Buienradar rain radar into one answer: " +
            "what do you need to wear?"
    open val aboutData = "Weather data by Open-Meteo · Radar by Buienradar · Maps by OpenStreetMap"
    open val version = { versionName: String -> "Version $versionName" }
    open val resetSetup = "Run setup again"

    open val notifChannelName = "Commute advice"
    open val notifChannelBody = "Tells you which jacket to bring before you leave."
    open val notifLegLine = { name: String, time: String, body: String -> "$name $time — $body" }
    open val notifDry = "dry"
    open val notifRainPct = { percent: Int -> "$percent% rain" }
    open val notifSnooze = "Remind me in an hour"
    open val notifNoData = "Could not fetch the forecast"

    open val speedUnit = "km/h"
    open val minutesShort = { minutes: Int -> "$minutes min" }
    open val hoursShort = { hours: String -> "$hours h" }
    open val today = "today"
    open val tomorrow = "tomorrow"
    open val dayNamesShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    open val dayLettersShort = listOf("M", "T", "W", "T", "F", "S", "S")

    companion object {
        fun of(language: Language): Strings = when (language) {
            Language.NL -> DutchStrings
            Language.EN -> Strings()
            Language.SYSTEM ->
                if (Locale.getDefault().language.equals("nl", true)) DutchStrings else Strings()
        }
    }
}

object DutchStrings : Strings() {

    override val locale: Locale = Locale.forLanguageTag("nl-NL")

    override val tagline = "Welke jas moet er mee?"
    override val back = "Terug"
    override val next = "Verder"
    override val done = "Klaar"
    override val cancel = "Annuleren"
    override val save = "Bewaren"
    override val delete = "Verwijderen"
    override val edit = "Aanpassen"
    override val add = "Toevoegen"
    override val refresh = "Verversen"
    override val retry = "Opnieuw"
    override val close = "Sluiten"
    override val home = "Thuis"
    override val work = "Werk"
    override val loading = "Bezig…"

    override val tabToday = "Vandaag"
    override val tabForecast = "Verwachting"
    override val tabAlerts = "Meldingen"
    override val tabSettings = "Instellingen"

    override val welcomeBody =
        "Stel één keer je thuis- en werkadres in. Daarna hoor je elke ochtend " +
            "of je een regenjas, een warme jas of allebei nodig hebt."
    override val welcomeStart = "Beginnen"
    override val stepOf = { step: Int, total: Int -> "Stap $step van $total" }

    override val setHomeTitle = "Waar woon je?"
    override val setHomeBody = "Zoek een plaats of sleep de kaart naar je voordeur."
    override val setWorkTitle = "Waar fiets je naartoe?"
    override val setWorkBody = "Werk, school, het station — waar je rit eindigt."
    override val setTimesTitle = "Wanneer fiets je?"
    override val setTimesBody =
        "De verwachting wordt precies voor deze vertrektijden doorgerekend. " +
            "Je kunt ze altijd aanpassen."
    override val setNotifyTitle = "Wanneer zullen we het zeggen?"
    override val setNotifyBody =
        "Je krijgt één melding met het oordeel voor beide ritten. " +
            "Later kun je er zoveel toevoegen als je wilt."
    override val notificationsAllowed = "Meldingen staan aan"
    override val finishGo = "Fietsweer openen"

    override val searchPlace = "Zoek een plaats"
    override val searchNoResults = "Niets gevonden"
    override val locationDenied = "Geen toestemming voor locatie"
    override val locationUnavailable = "Kon geen locatie bepalen"
    override val confirmLocation = "Deze plek gebruiken"
    override val dragMapHint = "Sleep de kaart om de speld te zetten"

    override val toWork = "Heenrit"
    override val toHome = "Terugrit"
    override val adviceRain = "Neem een regenjas mee"
    override val adviceMaybeRain = "Een regenjas is misschien verstandig"
    override val adviceVest = "Vestweer"
    override val adviceWinter = "Winterjas aan"
    override val adviceRainVest = "Regenjas én een vest"
    override val adviceRainWinter = "Winterjas én een regenjas"
    override val adviceMaybeRainVest = "Vest aan, regenjas misschien"
    override val adviceMaybeRainWinter = "Winterjas aan, regenjas misschien"
    override val adviceNone = "Korte mouwen"
    override val adviceNoneSub = "Droog en zacht op beide ritten"

    override val chipRainJacket = "Regenjas"
    override val chipVest = "Vest"
    override val chipWinter = "Winterjas"
    override val chipGloves = "Handschoenen"
    override val chipHat = "Muts of buff"
    override val chipWindy = "Harde wind"
    override val chipFrost = "Pas op voor gladheid"
    override val chipHot = "Bidon mee"
    override val chipHeavy = "Stevige bui"
    override val chipDark = "Licht aan"

    override val rightNow = "Nu"
    override val feelsLike = "voelt als"
    override val onTheBike = "op de fiets"
    override val chanceOfRain = "kans op nat"
    override val chanceOfRainShort = "kans nat"
    override val expectedRain = "Verwachte neerslag"
    override val wind = "Wind"
    override val headwind = "tegenwind"
    override val tailwind = "meewind"
    override val crosswind = "zijwind"
    override val arrival = "Aankomst"
    override val next24h = "Komende 24 uur"
    override val temperature = "Temperatuur"
    override val precipitation = "Neerslag"
    override val bestMomentTitle = "Beste moment om te vertrekken"
    override val bestMomentSub = "Binnen de speling rond je vertrektijd"
    override val bestMomentNoSlots = "Nog geen verwachting voor dit venster"
    override val onPlannedTime = "precies op je vertrektijd"
    override val minutesEarlier = { minutes: Int -> "$minutes min eerder" }
    override val minutesLater = { minutes: Int -> "$minutes min later" }
    override val lastUpdated = { time: String -> "Bijgewerkt om $time" }
    override val updating = "Bijwerken…"
    override val staleNotice = { time: String -> "Kon niet bijwerken \u2014 dit is de verwachting van $time" }
    override val updateFailedTitle = "Geen verbinding met de weerdienst"
    override val updateFailedBody = "Controleer je verbinding en probeer het opnieuw."
    override val noModelsTitle = "Geen bruikbare verwachting"
    override val noModelsBody = "De weermodellen gaven niets terug voor deze route."
    override val setupNeededTitle = "Stel eerst je route in"
    override val setupNeededBody = "Kies een thuis- en werklocatie om te beginnen."

    override val departureTimeline = "Vertrektijden, komende 24 uur"
    override val dryWindows = "Droge vensters"
    override val noDryWindows = "Geen aaneengesloten droog venster in de komende 24 uur."
    override val modelMatrix = "Wat zegt elk model?"
    override val modelMatrixSub = "Rij = weerdienst, kolom = vertrektijd"
    override val sources = "Bronnen"
    override val dailyOutlook = "De komende dagen"
    override val detailsFor = { time: String -> "Details voor vertrek $time" }
    override val verdict = "Eindoordeel"
    override val modelsSeeingRain = "Modellen die neerslag zien"
    override val agreement = "Eensgezindheid"
    override val agreeStrong = "modellen zijn het eens"
    override val agreeSome = "redelijk eens"
    override val agreeSplit = "sterk verdeeld — onzeker"
    override val ensembleChance = "Ensemblekans"
    override val outOfRange = "buiten bereik"
    override val radarLabel = "Regenradar"
    override val radarNoEcho = "geen echo onderweg"
    override val radarBeyond = "verder dan 2 uur vooruit"
    override val radarRain = { mmPerHour: String -> "neerslag op de route, tot $mmPerHour mm/u" }
    override val expectedOnTheWay = "Verwachte neerslag onderweg"
    override val avgMaxMm = { average: String, wettest: String -> "gemiddeld $average mm, natste model $wettest mm" }
    override val windAndFeel = "Wind en gevoelstemperatuur"
    override val unknown = "onbekend"
    override val legendDry = "droog"
    override val legendMostlyDry = "vrijwel droog"
    override val legendUncertain = "twijfel"
    override val legendLikelyWet = "kans op nat"
    override val legendWet = "nat"
    override val legendNight = "lichtere blokken = nacht"
    override val legendModelDry = "dit model houdt het droog"
    override val legendModelWet = "dit model voorspelt neerslag"
    override val combined = "Samen (incl. radar)"
    override val slackRoom = { duration: String -> "$duration speling" }

    override val riskDry = "Droog"
    override val riskAlmostDry = "Vrijwel zeker droog"
    override val riskEither = "Kan net goed gaan"
    override val riskLikelyWet = "Grote kans op nat"
    override val riskWet = "Je wordt nat"

    override val alertsTitle = "Meldingen"
    override val alertsEmpty = "Nog geen meldingen"
    override val alertsEmptyBody = "Voeg een moment toe, dan zeggen we wat er mee moet."
    override val newAlert = "Nieuwe melding"
    override val editAlert = "Melding aanpassen"
    override val alertLabel = "Naam"
    override val alertLabelHint = "Ochtendcheck"
    override val alertTime = "Tijd"
    override val alertDays = "Herhalen"
    override val alertCoverage = "Gaat over"
    override val coverageOutbound = "Heenrit"
    override val coverageReturn = "Terugrit"
    override val coverageBoth = "Beide ritten"
    override val onlyWhenNeeded = "Alleen melden als er een jas nodig is"
    override val onlyWhenNeededShort = "Alleen indien nodig"
    override val onlyWhenNeededBody = "Stil blijven op droge, zachte dagen."
    override val testNotification = "Testmelding sturen"
    override val everyDay = "Elke dag"
    override val weekdays = "Werkdagen"
    override val weekend = "Weekend"
    override val neverRepeats = "Nooit — kies minstens één dag"
    override val nextFire = { time: String -> "Volgende: $time" }
    override val permNotifications = "Meldingen zijn geblokkeerd"
    override val permNotificationsBody = "Android toont niets tot je ze toestaat."
    override val permExact = "Exacte alarmen staan uit"
    override val permExactBody = "Zonder deze kan een melding een paar minuten te laat komen."
    override val permBattery = "Batterijoptimalisatie staat aan"
    override val permBatteryBody = "Android kan meldingen uitstellen om stroom te sparen."
    override val grant = "Oplossen"
    override val deleteAlertConfirm = "Deze melding verwijderen?"

    override val settingsRoute = "Route"
    override val settingsTimes = "Vertrektijden"
    override val settingsRiding = "Fietsen"
    override val settingsAdvice = "Advies"
    override val settingsLook = "Uiterlijk"
    override val settingsWidget = "Widget"
    override val widgetAdd = "Widget toevoegen"
    override val widgetSizeNormal = "Normaal, 4 \u00d7 2"
    override val widgetSizeSlim = "Smal, 4 \u00d7 1"
    override val settingsAbout = "Over"
    override val outboundTime = "Vertrek van huis om"
    override val returnTime = "Vertrek van werk om"
    override val settingsFlex = "Speling rond het vertrek"
    override val flexEarlier = "Kan eerder weg"
    override val flexLater = "Kan later weg"
    override val flexNone = "niet"
    override val cyclingSpeed = "Tempo bij windstil"
    override val windAdjust = "Wind meerekenen in de rijtijd"
    override val paceOnRoad = "Tempo onderweg"
    override val stillAirShort = "windstil"
    override val rideTimeIs = { distance: String, minutes: Int -> "$distance km · $minutes min bij windstil" }
    override val whatIsWet = "Wat is \"nat\"?"
    override val wetEveryDrop = "Elke druppel"
    override val wetDrizzle = "Motregen mag"
    override val wetShower = "Echte bui"
    override val rainJacketFrom = "Regenjas vanaf"
    override val rainJacketFromValue = { percent: Int -> "$percent% kans op nat" }
    override val vestBelowLabel = "Vest onder"
    override val winterBelowLabel = "Winterjas onder"
    override val feltOnBike = { temperature: String -> "$temperature gevoeld op de fiets" }
    override val useRadarTitle = "Regenradar gebruiken"
    override val theme = "Thema"
    override val themeSystem = "Systeem"
    override val themeLight = "Licht"
    override val themeDark = "Donker"
    override val dynamicColour = "Kleuren van mijn achtergrond"
    override val language = "Taal"
    override val langSystem = "Systeem"
    override val mapStyleTitle = "Kaartstijl"
    override val mapAuto = "Volgt thema"
    override val mapLight = "Licht"
    override val mapDark = "Donker"
    override val mapSoft = "Zacht"
    override val aboutBody =
        "Fietsweer combineert elk weermodel dat Open-Meteo voor jouw route " +
            "serveert, twee ensembles en de regenradar van Buienradar tot één " +
            "antwoord: wat moet er mee?"
    override val aboutData = "Weerdata van Open-Meteo · Radar van Buienradar · Kaarten van OpenStreetMap"
    override val version = { versionName: String -> "Versie $versionName" }
    override val resetSetup = "Instellen opnieuw doorlopen"

    override val notifChannelName = "Fietsadvies"
    override val notifChannelBody = "Zegt welke jas mee moet voordat je vertrekt."
    override val notifLegLine = { name: String, time: String, body: String -> "$name $time — $body" }
    override val notifDry = "droog"
    override val notifRainPct = { percent: Int -> "$percent% kans" }
    override val notifSnooze = "Over een uur nog eens"
    override val notifNoData = "Kon de verwachting niet ophalen"

    override val speedUnit = "km/u"
    override val minutesShort = { minutes: Int -> "$minutes min" }
    override val hoursShort = { hours: String -> "$hours uur" }
    override val today = "vandaag"
    override val tomorrow = "morgen"
    override val dayNamesShort = listOf("ma", "di", "wo", "do", "vr", "za", "zo")
    override val dayLettersShort = listOf("M", "D", "W", "D", "V", "Z", "Z")
}
