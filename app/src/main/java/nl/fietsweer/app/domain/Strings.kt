package nl.fietsweer.app.domain

import nl.fietsweer.app.data.AccentColor
import nl.fietsweer.app.data.Language
import nl.fietsweer.app.data.ThemeMode
import java.util.Locale

open class Strings {

    open val locale: Locale = Locale.ENGLISH

    open val tagline = "Which jacket do you need today?"
    open val back = "Back"
    open val recentre = "Back to departure time"
    open val next = "Next"
    open val done = "Done"
    open val cancel = "Cancel"
    open val save = "Save"
    open val delete = "Delete"
    open val add = "Add"
    open val retry = "Try again"
    open val close = "Close"
    open val home = "Home"
    open val work = "Work"

    open val tabToday = "Today"
    open val tabForecast = "Forecast"
    open val tabMap = "Map"
    open val layerRain = "Rain"
    open val layerPollen = "Hay fever"
    open val layerAir = "Air quality"
    open val layerUv = "UV"
    open val legendLow = "low"
    open val legendHigh = "high"
    open val layers = "Map layers"
    open val zoomIn = "Zoom in"
    open val zoomOut = "Zoom out"
    open val play = "Play"
    open val pause = "Pause"
    open val tabAlerts = "Alerts"
    open val groupCommute = "Your commute"
    open val groupAdvice = "Advice"
    open val groupApp = "App"
    open val adviceSummary = { percent: Int -> "Rain jacket from $percent%" }
    open val alertsActive = { count: Int -> if (count == 0) "None on" else if (count == 1) "1 on" else "$count on" }
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
    open val chipScarf = "Scarf"
    open val chipHat = "Hat"

    open val rightNow = "Right now"
    open val neededOn = { day: String -> "Needed $day" }
    open val feelsLike = "feels like"
    open val onTheBike = "on the bike"
    open val chanceOfRainShort = "rain"
    open val wind = "Wind"
    open val headwind = "headwind"
    open val tailwind = "tailwind"
    open val crosswind = "crosswind"
    open val updating = "Updating…"
    open val staleNotice = { time: String -> "Could not update \u2014 this is the forecast from $time" }
    open val updateFailedTitle = "Could not reach the weather service"
    open val updateFailedBody = "Check your connection and try again."
    open val noModelsTitle = "No usable forecast"
    open val noModelsBody = "The weather models returned nothing for this route."
    open val setupNeededTitle = "Set your route first"
    open val setupNeededBody = "Pick a home and a work location to get started."


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
    open val widgetRowTitle = "One row"
    open val widgetRowSub = "Tap a style; the 4×1 widget uses it"
    open val nowWidgetTitle = "Weather now, 4×1"
    open val nowWidgetSub = "Temperature, feel and wind of the moment, in the colours of the weather"
    open val nowWidgetAdd = "Add weather now"
    open val widgetBiggerTitle = "Bigger and smaller"
    open val widgetBiggerSub = "Drag the widget taller for the chart and both rides, narrower for just the advice"
    open val settingsAbout = "About"
    open val outboundTime = "Leave home at"
    open val returnTime = "Leave work at"
    open val settingsFlex = "Slack around departure"
    open val flexEarlier = "Can leave earlier"
    open val flexLater = "Can leave later"
    open val flexNone = "not at all"
    open val cyclingSpeed = "Pace in still air"
    open val windAdjust = "Let the wind change the ride time"
    open val rideTimeIs = { distance: String, minutes: Int -> "$distance km · $minutes min in still air" }
    open val whatIsWet = "What counts as wet?"
    open val wetEveryDrop = "Every drop"
    open val wetDrizzle = "Drizzle is fine"
    open val wetShower = "Real shower"
    open val rainJacketFrom = "Rain jacket from"
    open val rainJacketFromValue = { percent: Int -> "$percent% chance of rain" }
    open val vestBelowLabel = "Fleece below"
    open val winterBelowLabel = "Winter coat below"
    open val handsAndHead = "Hands and head"
    open val handsAndHeadSub = "Switch off what you never wear"
    open val belowLabel = { item: String -> "$item below" }
    open val feltOnBike = { temperature: String -> "$temperature felt on the bike" }
    open val useRadarTitle = "Use the rain radar"
    open val theme = "Theme"
    open val themeSystem = "System"
    open val themeLight = "Light"
    open val themeDark = "Dark"
    fun themeName(mode: ThemeMode): String = when (mode) {
        ThemeMode.SYSTEM -> themeSystem
        ThemeMode.LIGHT -> themeLight
        ThemeMode.DARK -> themeDark
    }
    open val accentColour = "Accent colour"
    open fun accentName(accent: AccentColor): String = when (accent) {
        AccentColor.BRAND -> "Fietsweer"
        AccentColor.WALLPAPER -> "Wallpaper"
        AccentColor.BLUE -> "Blue"
        AccentColor.GREEN -> "Green"
        AccentColor.GOLD -> "Gold"
        AccentColor.COPPER -> "Copper"
        AccentColor.BORDEAUX -> "Bordeaux"
        AccentColor.ROSE -> "Rose red"
        AccentColor.PURPLE -> "Purple"
        AccentColor.INK -> "Ink"
    }
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
    open val compassPoints = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

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
    override val recentre = "Terug naar vertrektijd"
    override val next = "Verder"
    override val done = "Klaar"
    override val cancel = "Annuleren"
    override val save = "Bewaren"
    override val delete = "Verwijderen"
    override val add = "Toevoegen"
    override val retry = "Opnieuw"
    override val close = "Sluiten"
    override val home = "Thuis"
    override val work = "Werk"

    override val tabToday = "Vandaag"
    override val tabForecast = "Verwachting"
    override val tabMap = "Kaart"
    override val layerRain = "Regen"
    override val layerPollen = "Hooikoorts"
    override val layerAir = "Luchtkwaliteit"
    override val layerUv = "UV"
    override val legendLow = "laag"
    override val legendHigh = "hoog"
    override val layers = "Kaartlagen"
    override val zoomIn = "Inzoomen"
    override val zoomOut = "Uitzoomen"
    override val play = "Afspelen"
    override val pause = "Pauzeren"
    override val tabAlerts = "Meldingen"
    override val groupCommute = "Jouw rit"
    override val groupAdvice = "Advies"
    override val groupApp = "App"
    override val adviceSummary = { percent: Int -> "Regenjas vanaf $percent%" }
    override val alertsActive = { count: Int -> if (count == 0) "Geen aan" else "$count aan" }
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
    override val chipScarf = "Sjaal"
    override val chipHat = "Muts"

    override val rightNow = "Nu"
    override val neededOn = { day: String -> "$day nodig" }
    override val feelsLike = "voelt als"
    override val onTheBike = "op de fiets"
    override val chanceOfRainShort = "regen"
    override val wind = "Wind"
    override val headwind = "tegenwind"
    override val tailwind = "meewind"
    override val crosswind = "zijwind"
    override val updating = "Bijwerken…"
    override val staleNotice = { time: String -> "Kon niet bijwerken \u2014 dit is de verwachting van $time" }
    override val updateFailedTitle = "Geen verbinding met de weerdienst"
    override val updateFailedBody = "Controleer je verbinding en probeer het opnieuw."
    override val noModelsTitle = "Geen bruikbare verwachting"
    override val noModelsBody = "De weermodellen gaven niets terug voor deze route."
    override val setupNeededTitle = "Stel eerst je route in"
    override val setupNeededBody = "Kies een thuis- en werklocatie om te beginnen."


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
    override val widgetRowTitle = "Eén rij"
    override val widgetRowSub = "Tik een stijl aan; de 4×1-widget gebruikt die"
    override val nowWidgetTitle = "Weer nu, 4×1"
    override val nowWidgetSub = "Temperatuur, gevoel en wind van dit moment, in de kleuren van het weer"
    override val nowWidgetAdd = "Weer nu toevoegen"
    override val widgetBiggerTitle = "Groter en kleiner"
    override val widgetBiggerSub = "Sleep de widget hoger voor de grafiek en beide ritten, smaller voor alleen het advies"
    override val settingsAbout = "Over"
    override val outboundTime = "Vertrek van huis om"
    override val returnTime = "Vertrek van werk om"
    override val settingsFlex = "Speling rond het vertrek"
    override val flexEarlier = "Kan eerder weg"
    override val flexLater = "Kan later weg"
    override val flexNone = "niet"
    override val cyclingSpeed = "Tempo bij windstil"
    override val windAdjust = "Wind meerekenen in de rijtijd"
    override val rideTimeIs = { distance: String, minutes: Int -> "$distance km · $minutes min bij windstil" }
    override val whatIsWet = "Wat is \"nat\"?"
    override val wetEveryDrop = "Elke druppel"
    override val wetDrizzle = "Motregen mag"
    override val wetShower = "Echte bui"
    override val rainJacketFrom = "Regenjas vanaf"
    override val rainJacketFromValue = { percent: Int -> "$percent% kans op nat" }
    override val vestBelowLabel = "Vest onder"
    override val handsAndHead = "Handen en hoofd"
    override val handsAndHeadSub = "Zet uit wat je nooit draagt"
    override val belowLabel = { item: String -> "$item onder" }
    override val winterBelowLabel = "Winterjas onder"
    override val feltOnBike = { temperature: String -> "$temperature gevoeld op de fiets" }
    override val useRadarTitle = "Regenradar gebruiken"
    override val theme = "Thema"
    override val themeSystem = "Systeem"
    override val themeLight = "Licht"
    override val themeDark = "Donker"
    override val accentColour = "Accentkleur"
    override fun accentName(accent: AccentColor): String = when (accent) {
        AccentColor.BRAND -> "Fietsweer"
        AccentColor.WALLPAPER -> "Achtergrond"
        AccentColor.BLUE -> "Blauw"
        AccentColor.GREEN -> "Groen"
        AccentColor.GOLD -> "Goud"
        AccentColor.COPPER -> "Koper"
        AccentColor.BORDEAUX -> "Bordeaux"
        AccentColor.ROSE -> "Rozerood"
        AccentColor.PURPLE -> "Paars"
        AccentColor.INK -> "Inkt"
    }
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
    override val notifDry = "droog"
    override val notifRainPct = { percent: Int -> "$percent% kans" }
    override val notifSnooze = "Over een uur nog eens"
    override val notifNoData = "Kon de verwachting niet ophalen"

    override val speedUnit = "km/u"
    override val hoursShort = { hours: String -> "$hours uur" }
    override val today = "vandaag"
    override val tomorrow = "morgen"
    override val dayNamesShort = listOf("ma", "di", "wo", "do", "vr", "za", "zo")
    override val dayLettersShort = listOf("M", "D", "W", "D", "V", "Z", "Z")
    override val compassPoints = listOf("N", "NO", "O", "ZO", "Z", "ZW", "W", "NW")
}
