package nl.fietsweer.app.domain

import nl.fietsweer.app.data.Lang
import java.util.Locale

/**
 * All user-facing copy lives here rather than in strings.xml, so that
 * parameterised sentences stay type-safe and the language can be switched
 * inside the app without recreating the activity.
 */
open class Txt {

    open val locale: Locale = Locale.ENGLISH

    // -------------------------------------------------------------- general
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
    open val skip = "Skip"
    open val close = "Close"
    open val home = "Home"
    open val work = "Work"
    open val on = "On"
    open val off = "Off"
    open val change = "Change"
    open val loading = "Loading…"

    // ----------------------------------------------------------------- tabs
    open val tabToday = "Today"
    open val tabForecast = "Forecast"
    open val tabAlerts = "Alerts"
    open val tabSettings = "Settings"

    // ----------------------------------------------------------- onboarding
    open val welcomeTitle = "Fietsweer"
    open val welcomeBody =
        "Set your home and work once. After that this app tells you every " +
            "morning whether you need a rain jacket, a warm jacket, or both."
    open val welcomeStart = "Get started"
    open val stepOf = { a: Int, b: Int -> "Step $a of $b" }

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
    open val allowNotifications = "Allow notifications"
    open val notificationsAllowed = "Notifications are on"
    open val finishTitle = "All set"
    open val finishBody = "Fetching the first forecast…"
    open val finishGo = "Open Fietsweer"

    // ------------------------------------------------------------- map picker
    open val searchPlace = "Search for a place"
    open val searchNoResults = "Nothing found"
    open val useMyLocation = "Use my location"
    open val locationDenied = "Location permission was denied"
    open val locationUnavailable = "Could not get a location fix"
    open val confirmLocation = "Use this spot"
    open val dragMapHint = "Drag the map to place the pin"

    // ----------------------------------------------------------------- today
    open val toWork = "To work"
    open val toHome = "Back home"
    open val takeWithYou = "Take with you"
    open val nothingNeeded = "Nothing needed"
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
    open val expectedRain = "Expected rain"
    open val wind = "Wind"
    open val gusts = "gusts"
    open val headwind = "headwind"
    open val tailwind = "tailwind"
    open val crosswind = "crosswind"
    open val departure = "Departure"
    open val arrival = "Arrival"
    open val distance = "Distance"
    open val duration = "Ride time"
    open val next24h = "Next 24 hours"
    open val temperature = "Temperature"
    open val precipitation = "Rain"
    open val bestMomentTitle = "Best moment to leave"
    open val bestMomentSub = "Within the slack around your departure time"
    open val bestMomentNone = "No really dry window in the next 24 hours"
    open val bestMomentNoSlots = "No forecast for this window yet"
    open val onPlannedTime = "right on your departure time"
    open val minutesEarlier = { m: Int -> "$m min earlier" }
    open val minutesLater = { m: Int -> "$m min later" }
    open val leaveNow = "Leave now"
    open val lastUpdated = { s: String -> "Updated at $s" }
    open val updating = "Updating…"
    open val staleNotice = { s: String -> "Could not update \u2014 this is the forecast from $s" }
    open val updateFailedTitle = "Could not reach the weather service"
    open val updateFailedBody = "Check your connection and try again."
    open val noModelsTitle = "No usable forecast"
    open val noModelsBody = "The weather models returned nothing for this route."
    open val setupNeededTitle = "Set your route first"
    open val setupNeededBody = "Pick a home and a work location to get started."

    // -------------------------------------------------------------- forecast
    open val departureTimeline = "Departure times, next 24 hours"
    open val dryWindows = "Dry windows"
    open val noDryWindows = "No uninterrupted dry window in the next 24 hours."
    open val modelMatrix = "What does each model say?"
    open val modelMatrixSub = "Row = weather service, column = departure time"
    open val sources = "Sources"
    open val dailyOutlook = "The next few days"
    open val detailsFor = { s: String -> "Details for leaving at $s" }
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
    open val radarRain = { mm: String -> "rain on the route, up to $mm mm/h" }
    open val expectedOnTheWay = "Rain expected on the way"
    open val avgMaxMm = { a: String, b: String -> "average $a mm, wettest model $b mm" }
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
    open val slackRoom = { s: String -> "$s of slack" }

    open val riskDry = "Dry"
    open val riskAlmostDry = "Almost certainly dry"
    open val riskEither = "Could go either way"
    open val riskLikelyWet = "Good chance of getting wet"
    open val riskWet = "You will get wet"

    // ---------------------------------------------------------------- alerts
    open val alertsTitle = "Notifications"
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
    open val nextFire = { s: String -> "Next: $s" }
    open val permNotifications = "Notifications are blocked"
    open val permNotificationsBody = "Android will not show alerts until you allow them."
    open val permExact = "Exact alarms are off"
    open val permExactBody = "Without them a notification can be a few minutes late."
    open val permBattery = "Battery optimisation is on"
    open val permBatteryBody = "Android may delay notifications to save power."
    open val grant = "Fix this"
    open val deleteAlertConfirm = "Delete this notification?"

    // -------------------------------------------------------------- settings
    open val settingsRoute = "Route"
    open val settingsTimes = "Ride times"
    open val settingsRiding = "Riding"
    open val settingsAdvice = "When to warn me"
    open val settingsLook = "Appearance"
    open val settingsWidget = "Home screen"
    open val widgetAdd = "Add the widget"
    open val widgetSizeNormal = "Normal, 4 \u00d7 2"
    open val widgetSizeSlim = "Slim, 4 \u00d7 1"
    open val widgetResizeHint =
        "Long-press the widget on your home screen and drag its edges to switch size."
    open val widgetAddDone = "Check your home screen"
    open val widgetUnsupported = "Add it from your launcher's widget list"
    open val settingsAbout = "About"
    open val outboundTime = "Leave home at"
    open val returnTime = "Leave work at"
    open val settingsFlex = "Slack around departure"
    open val flexEarlier = "Can leave earlier"
    open val flexLater = "Can leave later"
    open val flexNone = "not at all"
    open val flexBody =
        "How far you may shift from a planned time. The best moment is looked " +
            "for inside that window, and once the window has passed the ride " +
            "makes way for the next day's."
    open val cyclingSpeed = "Pace in still air"
    open val cyclingSpeedBody =
        "Your own speed with no wind at all. Head- and tailwind are added on top."
    open val windAdjust = "Let the wind change the ride time"
    open val windAdjustBody =
        "Headwind slows you down, tailwind carries you, and the arrival times follow."
    open val paceOnRoad = "Pace on the road"
    /** e.g. "+14 min headwind · 13 instead of 19 km/h". */
    open val windTimeLine = { delta: String, relation: String, pace: String, still: String ->
        "$delta $relation · $pace instead of $still"
    }
    open val stillAirShort = "still air"
    open val rideTimeIs = { km: String, min: Int -> "$km km · $min min in still air" }
    open val whatIsWet = "What counts as wet?"
    open val wetEveryDrop = "Every drop"
    open val wetDrizzle = "Drizzle is fine"
    open val wetShower = "Real shower"
    open val rainJacketFrom = "Rain jacket from"
    open val rainJacketFromValue = { p: Int -> "$p% chance of rain" }
    open val vestBelowLabel = "Fleece below"
    open val winterBelowLabel = "Winter coat below"
    open val feltOnBike = { t: String -> "$t felt on the bike" }
    open val layerLadder = { vest: String, winter: String ->
        "Above $vest short sleeves, below $winter a winter coat, in between a fleece."
    }
    open val useRadarTitle = "Use the rain radar"
    open val useRadarBody = "Buienradar nowcast, Netherlands and Belgium only."
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
    open val version = { v: String -> "Version $v" }
    open val resetSetup = "Run setup again"

    // ---------------------------------------------------------- notification
    open val notifChannelName = "Commute advice"
    open val notifChannelBody = "Tells you which jacket to bring before you leave."
    open val notifNothing = "Nothing to bring"
    open val notifLegLine = { name: String, time: String, body: String -> "$name $time — $body" }
    open val notifDry = "dry"
    open val notifRainPct = { p: Int -> "$p% rain" }
    open val notifSnooze = "Remind me in an hour"
    open val notifOpen = "Open"
    open val notifNoData = "Could not fetch the forecast"

    // ------------------------------------------------------------- formatting
    open val speedUnit = "km/h"
    open val minutesShort = { m: Int -> "$m min" }
    open val hoursShort = { h: String -> "$h h" }
    open val today = "today"
    open val tomorrow = "tomorrow"
    open val dayNamesShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    open val dayLettersShort = listOf("M", "T", "W", "T", "F", "S", "S")

    companion object {
        fun of(lang: Lang): Txt = when (lang) {
            Lang.NL -> NlTxt
            Lang.EN -> Txt()
            Lang.SYSTEM ->
                if (Locale.getDefault().language.equals("nl", true)) NlTxt else Txt()
        }
    }
}

object NlTxt : Txt() {

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
    override val skip = "Overslaan"
    override val close = "Sluiten"
    override val home = "Thuis"
    override val work = "Werk"
    override val on = "Aan"
    override val off = "Uit"
    override val change = "Wijzigen"
    override val loading = "Bezig…"

    override val tabToday = "Vandaag"
    override val tabForecast = "Verwachting"
    override val tabAlerts = "Meldingen"
    override val tabSettings = "Instellingen"

    override val welcomeBody =
        "Stel één keer je thuis- en werkadres in. Daarna hoor je elke ochtend " +
            "of je een regenjas, een warme jas of allebei nodig hebt."
    override val welcomeStart = "Beginnen"
    override val stepOf = { a: Int, b: Int -> "Stap $a van $b" }

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
    override val allowNotifications = "Meldingen toestaan"
    override val notificationsAllowed = "Meldingen staan aan"
    override val finishTitle = "Klaar om te fietsen"
    override val finishBody = "De eerste verwachting wordt opgehaald…"
    override val finishGo = "Fietsweer openen"

    override val searchPlace = "Zoek een plaats"
    override val searchNoResults = "Niets gevonden"
    override val useMyLocation = "Mijn locatie gebruiken"
    override val locationDenied = "Geen toestemming voor locatie"
    override val locationUnavailable = "Kon geen locatie bepalen"
    override val confirmLocation = "Deze plek gebruiken"
    override val dragMapHint = "Sleep de kaart om de speld te zetten"

    override val toWork = "Heenrit"
    override val toHome = "Terugrit"
    override val takeWithYou = "Neem mee"
    override val nothingNeeded = "Niets nodig"
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
    override val expectedRain = "Verwachte neerslag"
    override val wind = "Wind"
    override val gusts = "vlagen"
    override val headwind = "tegenwind"
    override val tailwind = "meewind"
    override val crosswind = "zijwind"
    override val departure = "Vertrek"
    override val arrival = "Aankomst"
    override val distance = "Afstand"
    override val duration = "Rijtijd"
    override val next24h = "Komende 24 uur"
    override val temperature = "Temperatuur"
    override val precipitation = "Neerslag"
    override val bestMomentTitle = "Beste moment om te vertrekken"
    override val bestMomentSub = "Binnen de speling rond je vertrektijd"
    override val bestMomentNone = "Geen echt droog venster in de komende 24 uur"
    override val bestMomentNoSlots = "Nog geen verwachting voor dit venster"
    override val onPlannedTime = "precies op je vertrektijd"
    override val minutesEarlier = { m: Int -> "$m min eerder" }
    override val minutesLater = { m: Int -> "$m min later" }
    override val leaveNow = "Nu meteen"
    override val lastUpdated = { s: String -> "Bijgewerkt om $s" }
    override val updating = "Bijwerken…"
    override val staleNotice = { s: String -> "Kon niet bijwerken \u2014 dit is de verwachting van $s" }
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
    override val detailsFor = { s: String -> "Details voor vertrek $s" }
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
    override val radarRain = { mm: String -> "neerslag op de route, tot $mm mm/u" }
    override val expectedOnTheWay = "Verwachte neerslag onderweg"
    override val avgMaxMm = { a: String, b: String -> "gemiddeld $a mm, natste model $b mm" }
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
    override val slackRoom = { s: String -> "$s speling" }

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
    override val nextFire = { s: String -> "Volgende: $s" }
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
    override val settingsAdvice = "Wanneer waarschuwen"
    override val settingsLook = "Uiterlijk"
    override val settingsWidget = "Startscherm"
    override val widgetAdd = "Widget toevoegen"
    override val widgetSizeNormal = "Normaal, 4 \u00d7 2"
    override val widgetSizeSlim = "Smal, 4 \u00d7 1"
    override val widgetResizeHint =
        "Houd de widget op je startscherm ingedrukt en sleep de randen om te wisselen."
    override val widgetAddDone = "Kijk op je startscherm"
    override val widgetUnsupported = "Voeg hem toe via de widgetlijst van je launcher"
    override val settingsAbout = "Over"
    override val outboundTime = "Vertrek van huis om"
    override val returnTime = "Vertrek van werk om"
    override val settingsFlex = "Speling rond het vertrek"
    override val flexEarlier = "Kan eerder weg"
    override val flexLater = "Kan later weg"
    override val flexNone = "niet"
    override val flexBody =
        "Hoeveel je mag schuiven met een vertrektijd. Binnen dat venster wordt " +
            "het beste moment gezocht, en zodra het voorbij is maakt de rit " +
            "plaats voor die van de volgende dag."
    override val cyclingSpeed = "Tempo bij windstil"
    override val cyclingSpeedBody =
        "Je eigen snelheid als het helemaal windstil is. Kop- en meewind komen daar bovenop."
    override val windAdjust = "Wind meerekenen in de rijtijd"
    override val windAdjustBody =
        "Tegenwind remt je af, meewind duwt je vooruit, en de aankomsttijden lopen mee."
    override val paceOnRoad = "Tempo onderweg"
    override val windTimeLine = { delta: String, relation: String, pace: String, still: String ->
        "$delta $relation · $pace i.p.v. $still"
    }
    override val stillAirShort = "windstil"
    override val rideTimeIs = { km: String, min: Int -> "$km km · $min min bij windstil" }
    override val whatIsWet = "Wat is \"nat\"?"
    override val wetEveryDrop = "Elke druppel"
    override val wetDrizzle = "Motregen mag"
    override val wetShower = "Echte bui"
    override val rainJacketFrom = "Regenjas vanaf"
    override val rainJacketFromValue = { p: Int -> "$p% kans op nat" }
    override val vestBelowLabel = "Vest onder"
    override val winterBelowLabel = "Winterjas onder"
    override val feltOnBike = { t: String -> "$t gevoeld op de fiets" }
    override val layerLadder = { vest: String, winter: String ->
        "Boven $vest korte mouwen, onder $winter een winterjas, daartussen een vest."
    }
    override val useRadarTitle = "Regenradar gebruiken"
    override val useRadarBody = "Buienradar-nowcast, alleen Nederland en België."
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
    override val version = { v: String -> "Versie $v" }
    override val resetSetup = "Instellen opnieuw doorlopen"

    override val notifChannelName = "Fietsadvies"
    override val notifChannelBody = "Zegt welke jas mee moet voordat je vertrekt."
    override val notifNothing = "Niets nodig"
    override val notifLegLine = { name: String, time: String, body: String -> "$name $time — $body" }
    override val notifDry = "droog"
    override val notifRainPct = { p: Int -> "$p% kans" }
    override val notifSnooze = "Over een uur nog eens"
    override val notifOpen = "Openen"
    override val notifNoData = "Kon de verwachting niet ophalen"

    override val speedUnit = "km/u"
    override val minutesShort = { m: Int -> "$m min" }
    override val hoursShort = { h: String -> "$h uur" }
    override val today = "vandaag"
    override val tomorrow = "morgen"
    override val dayNamesShort = listOf("ma", "di", "wo", "do", "vr", "za", "zo")
    override val dayLettersShort = listOf("M", "D", "W", "D", "V", "Z", "Z")
}
