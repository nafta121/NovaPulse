package com.example.core.ai

import com.example.feature.widget.BriefingSession

/**
 * Dynamic Content Provider managing header summaries and a rich 5-6 card
 * Lifestyle Carousel styled after One UI 9.
 */
object BriefingContentProvider {

    /**
     * Get a dynamic Header summary by Session, automatically inserting actual temperature (${temp}°C) and weather (${weather}).
     */
    fun getFallbackRecap(sessionName: String, temp: Int, weatherDesc: String): String {
        val session = BriefingSession.values().firstOrNull {
            it.sessionTag.equals(sessionName, ignoreCase = true) || it.name.equals(sessionName, ignoreCase = true)
        } ?: BriefingSession.getCurrentSession()
        return getHeaderSummary(session, temp, weatherDesc)
    }

    fun getHeaderSummary(session: BriefingSession, temp: Int, weather: String): String {
        val weatherClean = weather.lowercase()
        val tempStr = "${temp}°C"

        val options = when (session) {
            BriefingSession.EARLY_MORNING -> listOf(
                "Fresh dawn air! It's $tempStr and $weatherClean. Start with a glass of warm water to wake up your body.",
                "Good morning! Pleasant $tempStr weather—a great time for light stretching and deep breaths.",
                "Ready for a bright new day! It's $tempStr outside, wishing you an energized and productive day ahead.",
                "Gentle morning sunshine! $tempStr and $weatherClean. Enjoy a cup of coffee and kickstart your morning."
            )

            BriefingSession.MID_MORNING -> listOf(
                "Golden hours for deep focus! $tempStr and $weatherClean. Tackle your most important task right now.",
                "Productive morning flow! It's $tempStr—keep your momentum going and hit your daily goals.",
                "Sharp and focused! Cool $tempStr air outside, breeze through your to-do list today.",
                "Peak performance window! $tempStr and $weatherClean. Confidently conquer your most challenging tasks."
            )

            BriefingSession.NOON -> listOf(
                "Time to refuel! It's $tempStr outside—enjoy a delicious and nutritious lunch break.",
                "Midday recharge! Outdoor temperature is $tempStr, rest your eyes after a busy morning of work.",
                "Take a 15-20 minute power nap at noon to get ready for a high-energy afternoon.",
                "Midday energy boost! $tempStr and $weatherClean, savor your lunch and unwind your mind."
            )

            BriefingSession.AFTERNOON -> listOf(
                "Keep your afternoon energy up! It's $tempStr—grab some water or tea to stay refreshed.",
                "Power through! $tempStr and $weatherClean outside, wrap up your remaining tasks for the day.",
                "Do some light desk stretches to release tension and maintain peak work productivity.",
                "Stay sharp this afternoon! It's $tempStr, sip some afternoon tea and keep a smooth workflow."
            )

            BriefingSession.EVENING -> listOf(
                "Sunset is here! Pleasant $tempStr breeze—set work aside and enjoy your evening wind-down.",
                "Another hardworking day complete! $tempStr and $weatherClean, head home and relax with loved ones.",
                "Comfortable sunset weather at $tempStr! Take a light walk or stretch to melt away fatigue.",
                "Relaxing late afternoon! It's $tempStr, clear your mind and get ready for a warm dinner."
            )

            BriefingSession.NIGHT -> listOf(
                "Peaceful evening! Cool $tempStr air—take time for self-care and light entertainment.",
                "Prepare for deep sleep! $tempStr and $weatherClean, dim your screens for full overnight recovery.",
                "Quiet night ahead! It's $tempStr, enjoy a cozy dinner and unwind with your family.",
                "Breezy night sky at $tempStr! Sip some warm herbal tea and enjoy the calm atmosphere."
            )

            BriefingSession.LATE_NIGHT -> listOf(
                "Calm late night! $tempStr and $weatherClean. Relax your mind and prepare for deep, restful sleep.",
                "Time to rest! Cool $tempStr air outside, set aside all worries and let your body fully recover.",
                "Gentle night breeze at $tempStr! Keep your bedroom airy and have a wonderful night's sleep.",
                "Quality sleep time! It's $tempStr, put away your phone early to recharge 100% for tomorrow morning."
            )
        }

        return options.random()
    }

    /**
     * Ultra-concise summary set (Compact Briefing) designed SPECIFICALLY for the Dynamic Island
     * Ensures a clean fit inside the Island pill without overflowing or truncating.
     */
    fun getIslandSummary(session: BriefingSession, temp: Int, weather: String = "pleasant"): String {
        val options = when (session) {
            BriefingSession.EARLY_MORNING -> listOf(
                "Rise and shine with energy!",
                "Kickstart a joyful new day!",
                "Welcome the fresh morning dawn!",
                "Crisp morning air and sunshine!",
                "Wishing you a wonderful day!"
            )

            BriefingSession.MID_MORNING -> listOf(
                "Golden hours for deep focus!",
                "Stay locked in on your work!",
                "Unleash peak productivity now!",
                "Have a super productive day!",
                "Keep your work momentum strong!",
                "Confidently crush your goals!"
            )

            BriefingSession.NOON -> listOf(
                "Lunch break—time to recharge!",
                "Enjoy a delicious midday meal!",
                "Take a quick midday power nap!",
                "Rest your eyes and unwind!",
                "Refuel for the afternoon ahead!"
            )

            BriefingSession.AFTERNOON -> listOf(
                "Keep your afternoon energy high!",
                "Hydrate with water or afternoon tea!",
                "Focus up for the final sprint!",
                "Have a productive afternoon!",
                "Keep up your great momentum!"
            )

            BriefingSession.EVENING -> listOf(
                "Wrap up work and head home!",
                "Clock out and unwind tonight!",
                "Cool and breezy sunset vibes!",
                "Enjoy a warm, cozy dinner!",
                "Relax your mind this evening!"
            )

            BriefingSession.NIGHT -> listOf(
                "Have a cozy, peaceful night!",
                "Unwind and enjoy your evening!",
                "Take some time for yourself!",
                "Put on some soft, relaxing music!",
                "Enjoy the calm night atmosphere!"
            )

            BriefingSession.LATE_NIGHT -> listOf(
                "Screens off, time for bed!",
                "Wishing you a restful night!",
                "It's late—time to rest up!",
                "Sweet dreams tonight!",
                "Set aside all worries and sleep!"
            )
        }

        return options.random()
    }

    /**
     * Get a rich, categorized list of 5-6 randomized Lifestyle Carousel recommendation cards per session.
     * Automatically adapts to actual weather/temperature (hot >32°C vs cool <28°C).
     */
    fun getLifestyleTips(
        session: BriefingSession,
        temp: Int,
        weather: String,
        count: Int = 6
    ): List<LifestyleTip> {
        val currentTemp = temp
        val tempStr = "${currentTemp}°C"
        val isHot = currentTemp > 32
        val isCool = currentTemp < 28

        val pool = when (session) {
            BriefingSession.EARLY_MORNING, BriefingSession.MID_MORNING -> listOf(
                // Card 1: Morning Dining
                LifestyleTip(
                    category = "Food & Dining",
                    tag = "Breakfast",
                    title = "Warm Artisan Noodle Soup",
                    description = "A steaming, protein-rich bowl to fuel your entire morning.",
                    emoji = "🍜"
                ),
                LifestyleTip(
                    category = "Food & Dining",
                    tag = "Breakfast",
                    title = "Crispy Sunny-Side Baguette",
                    description = "A quick, crunchy, and energizing morning breakfast.",
                    emoji = "🥖"
                ),

                // Card 2: Temperature-Adaptive Drink
                LifestyleTip(
                    category = "Drinks",
                    tag = if (isHot) "Cooling" else "Energy Boost",
                    title = if (isHot) "Chilled Watermelon Juice" else "Rich Salted Cream Coffee",
                    description = if (isHot) "It's a warm $tempStr—cool down and hydrate instantly." else "Pleasant $tempStr weather—savor a cup of coffee to stay sharp.",
                    emoji = if (isHot) "🍉" else "☕"
                ),

                // Card 3: Fitness / Health
                LifestyleTip(
                    category = "Health",
                    tag = "Deep Breath",
                    title = "5-Minute Deep Breathing",
                    description = "At $tempStr, take 5 minutes of deep breathing to center your mind.",
                    emoji = "🧘‍♂️"
                ),
                LifestyleTip(
                    category = "Fitness",
                    tag = "Wellness",
                    title = if (isCool) "Morning Breeze Jog" else "5-Minute Morning Stretch",
                    description = if (isCool) "Crisp $tempStr air—ideal for a 15-minute outdoor jog." else "Gently wake up your body and relieve neck and shoulder tension.",
                    emoji = if (isCool) "🏃‍♂️" else "🧘‍♂️"
                ),

                // Card 4: Mindfulness / Growth
                LifestyleTip(
                    category = "Mindfulness",
                    tag = "Growth",
                    title = "Personal Growth Podcast",
                    description = "Tune into 10 minutes of positive insights before starting work.",
                    emoji = "🎙️"
                ),

                // Card 5: Work / Productivity Tip
                LifestyleTip(
                    category = "Productivity",
                    tag = "Focus",
                    title = "25-Minute Pomodoro Sprint",
                    description = "Lock in with deep focus to tackle your top priority.",
                    emoji = "⏱️"
                ),

                // Card 6: Lifestyle
                LifestyleTip(
                    category = "Lifestyle",
                    tag = "Hydrate",
                    title = "Drink 500ml of Warm Water",
                    description = "Kickstart your metabolism and rehydrate first thing in the morning.",
                    emoji = "🥛"
                )
            )

            BriefingSession.NOON -> listOf(
                // Card 1: Midday Dining
                LifestyleTip(
                    category = "Food & Dining",
                    tag = "Lunch",
                    title = "Grilled Pork Rice Platter",
                    description = "Enjoy a balanced lunch to recharge your midday energy.",
                    emoji = "🍛"
                ),
                LifestyleTip(
                    category = "Food & Dining",
                    tag = "Refreshing",
                    title = "Fresh Grilled Vermicelli & Broth",
                    description = "A light, refreshing, and flavorful midday lunch option.",
                    emoji = "🍲"
                ),

                // Card 2: Midday Drink
                LifestyleTip(
                    category = "Drinks",
                    tag = "Cooling",
                    title = if (isHot) "Fresh Chilled Coconut Water" else "Iced Peach Lemongrass Tea",
                    description = "Outdoor temp is $tempStr—replenish natural electrolytes.",
                    emoji = if (isHot) "🥥" else "🍹"
                ),

                // Card 3: Rest / Recovery
                LifestyleTip(
                    category = "Health",
                    tag = "Midday Rest",
                    title = "15-20 Minute Power Nap",
                    description = "Let your brain rest and reset for a productive afternoon.",
                    emoji = "💤"
                ),

                // Card 4: Relaxation / Deep Breathing
                LifestyleTip(
                    category = "Health",
                    tag = "Deep Breath",
                    title = "5-Minute Deep Breathing",
                    description = "At $tempStr, take 5 minutes of deep breathing to center your mind.",
                    emoji = "🧘‍♀️"
                ),
                LifestyleTip(
                    category = "Relaxation",
                    tag = "Recovery",
                    title = "Eye Care & 4-7-8 Breathing",
                    description = "Relieve eye strain after hours of looking at screens.",
                    emoji = "👀"
                ),

                // Card 5: Productivity
                LifestyleTip(
                    category = "Productivity",
                    tag = "Quick Tip",
                    title = "Tidy Up Your Workspace",
                    description = "A clean desk brings fresh clarity for the afternoon session.",
                    emoji = "🧹"
                ),

                // Card 6: Lifestyle
                LifestyleTip(
                    category = "Lifestyle",
                    tag = "Shade",
                    title = "Relax in a Shaded Spot",
                    description = "It's $tempStr outside—avoid harsh midday sun exposure.",
                    emoji = "🌿"
                )
            )

            BriefingSession.AFTERNOON -> listOf(
                // Card 1: Afternoon Snack
                LifestyleTip(
                    category = "Food & Dining",
                    tag = "Light Bite",
                    title = "Toasted Croissant & Avocado Smoothie",
                    description = "A tasty afternoon snack to restore your mental alertness.",
                    emoji = "🥑"
                ),

                // Card 2: Afternoon Drink
                LifestyleTip(
                    category = "Drinks",
                    tag = "Afternoon Tea",
                    title = "Refreshing Peach Citrus Tea",
                    description = "Crisp fruit tea for an invigorating afternoon pick-me-up.",
                    emoji = "🍹"
                ),

                // Card 3: Movement
                LifestyleTip(
                    category = "Health",
                    tag = "Deep Breath",
                    title = "5-Minute Deep Breathing",
                    description = "At $tempStr, take 5 minutes of deep breathing to center your mind.",
                    emoji = "🧘‍♂️"
                ),
                LifestyleTip(
                    category = "Fitness",
                    tag = "Stretching",
                    title = "3-Minute Desk Stretch",
                    description = "Stretch your back and shoulders to boost circulation.",
                    emoji = "🤸‍♂️"
                ),

                // Card 4: Mental Relaxation
                LifestyleTip(
                    category = "Mindfulness",
                    tag = "Music",
                    title = "Instrumental Lofi Beats",
                    description = "Gentle lofi melodies to sustain smooth afternoon focus.",
                    emoji = "🎧"
                ),

                // Card 5: Productivity
                LifestyleTip(
                    category = "Productivity",
                    tag = "Quick Tip",
                    title = "Drink 350ml of Pure Water",
                    description = "Stay hydrated and keep your energy steady all afternoon.",
                    emoji = "💧"
                ),

                // Card 6: Relaxation
                LifestyleTip(
                    category = "Relaxation",
                    tag = "Reading",
                    title = "Read 5 Pages of a Favorite Book",
                    description = "Step away from screens for 10 minutes to rest your mind.",
                    emoji = "📖"
                )
            )

            BriefingSession.EVENING -> listOf(
                // Card 1: Evening Dining
                LifestyleTip(
                    category = "Food & Dining",
                    tag = "Dinner",
                    title = "Light Herbal Mushroom Hotpot",
                    description = "Enjoy a wholesome, soothing dinner to refuel after work.",
                    emoji = "🍲"
                ),

                // Card 2: Evening Drink
                LifestyleTip(
                    category = "Drinks",
                    tag = "Calming",
                    title = "Warm Chamomile Herbal Tea",
                    description = "Sip warm tea to soothe your nerves after a busy workday.",
                    emoji = "🍵"
                ),

                // Card 3: Movement
                LifestyleTip(
                    category = "Fitness",
                    tag = "Walking",
                    title = "15-Minute Park Stroll",
                    description = "Pleasant $tempStr air—a light walk aids digestion and relaxation.",
                    emoji = "🚶‍♂️"
                ),
                LifestyleTip(
                    category = "Health",
                    tag = "Deep Breath",
                    title = "5-Minute Deep Breathing",
                    description = "At $tempStr, take 5 minutes of deep breathing to center your mind.",
                    emoji = "🧘‍♂️"
                ),

                // Card 4: Mindfulness
                LifestyleTip(
                    category = "Mindfulness",
                    tag = "Mindfulness",
                    title = "After-Work Mental Unwind",
                    description = "Leave work deadlines behind and transition into home life.",
                    emoji = "🌇"
                ),

                // Card 5: Lifestyle
                LifestyleTip(
                    category = "Lifestyle",
                    tag = "Relaxation",
                    title = "Sunset Evening Bike Ride",
                    description = "Comfortable $tempStr weather—enjoy a breezy evening ride.",
                    emoji = "🚴‍♂️"
                ),

                // Card 6: Preparation
                LifestyleTip(
                    category = "Lifestyle",
                    tag = "Prep",
                    title = "Prepare Essentials for Tomorrow",
                    description = "Lay out your outfit and bag for a stress-free morning.",
                    emoji = "👕"
                )
            )

            BriefingSession.NIGHT, BriefingSession.LATE_NIGHT -> listOf(
                // Card 1: Light Late-Night Snack
                LifestyleTip(
                    category = "Food & Dining",
                    tag = "Light Bite",
                    title = "Warm Milk & Whole Grain Nuts",
                    description = "An easy-to-digest snack that promotes deep, restful sleep.",
                    emoji = "🥛"
                ),

                // Card 2: Drink
                LifestyleTip(
                    category = "Drinks",
                    tag = "Soothing",
                    title = "Warm Honey Chamomile Tea",
                    description = "A calming bedtime drink to prepare your body for sleep.",
                    emoji = "🍵"
                ),

                // Card 3: Mental Unwind
                LifestyleTip(
                    category = "Mindfulness",
                    tag = "Sleep Hygiene",
                    title = "Turn Off Blue Light Screens",
                    description = "Set your phone aside 30 minutes before going to bed.",
                    emoji = "📱"
                ),

                // Card 4: Health
                LifestyleTip(
                    category = "Health",
                    tag = "Meditation",
                    title = "5-Minute Bedtime Meditation",
                    description = "Focus on slow, steady breaths to recharge for tomorrow.",
                    emoji = "🧘‍♀️"
                ),
                LifestyleTip(
                    category = "Health",
                    tag = "Deep Breath",
                    title = "5-Minute Deep Breathing",
                    description = "At $tempStr, take 5 minutes of deep breathing to center your mind.",
                    emoji = "🧘‍♂️"
                ),

                // Card 5: Light Entertainment
                LifestyleTip(
                    category = "Mindfulness",
                    tag = "Music",
                    title = "Soft Lofi & Rain Sounds",
                    description = "Gentle ambient sounds to help your mind completely unwind.",
                    emoji = "🎧"
                ),

                // Card 6: Lifestyle
                LifestyleTip(
                    category = "Lifestyle",
                    tag = "Bedroom",
                    title = "Keep Your Bedroom Cool & Airy",
                    description = "Cool $tempStr night—ideal for uninterrupted, restorative sleep.",
                    emoji = "🌙"
                )
            )
        }

        return pool.shuffled().take(count)
    }
}
