package com.example.konteksto.domain.model

data class ConferenceThemeBanner(
    val id: String,
    val categoryName: String,
    val imageUrl: String,
) {
    companion object {
        val DefaultCategories = listOf(
            ConferenceThemeBanner(
                id = "business_leadership",
                categoryName = "Business & Leadership",
                imageUrl = "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=800&q=80",
            ),
            ConferenceThemeBanner(
                id = "health_medicine",
                categoryName = "Health & Medicine",
                imageUrl = "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=800&q=80",
            ),
            ConferenceThemeBanner(
                id = "education_learning",
                categoryName = "Education & Learning",
                imageUrl = "https://images.unsplash.com/photo-1524178232363-1fb2b075b655?auto=format&fit=crop&w=800&q=80",
            ),
            ConferenceThemeBanner(
                id = "arts_creative_design",
                categoryName = "Arts & Creative Design",
                imageUrl = "https://images.unsplash.com/photo-1513364776144-60967b0f800f?auto=format&fit=crop&w=800&q=80",
            ),
            ConferenceThemeBanner(
                id = "climate_environment",
                categoryName = "Climate & Environment",
                imageUrl = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=800&q=80",
            ),
            ConferenceThemeBanner(
                id = "community_society",
                categoryName = "Community & Society",
                imageUrl = "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=800&q=80",
            ),
            ConferenceThemeBanner(
                id = "startups_ventures",
                categoryName = "Startups & Ventures",
                imageUrl = "https://images.unsplash.com/photo-1559136555-9303baea8ebd?auto=format&fit=crop&w=800&q=80",
            ),
            ConferenceThemeBanner(
                id = "science_discovery",
                categoryName = "Science & Discovery",
                imageUrl = "https://images.unsplash.com/photo-1532094349884-543bc11b234d?auto=format&fit=crop&w=800&q=80",
            ),
            ConferenceThemeBanner(
                id = "media_storytelling",
                categoryName = "Media & Storytelling",
                imageUrl = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=800&q=80",
            ),
            ConferenceThemeBanner(
                id = "policy_governance",
                categoryName = "Policy & Governance",
                imageUrl = "https://images.unsplash.com/photo-1541872703-74c5e44368f9?auto=format&fit=crop&w=800&q=80",
            ),
            ConferenceThemeBanner(
                id = "sports_performance",
                categoryName = "Sports & Performance",
                imageUrl = "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=800&q=80",
            ),
            ConferenceThemeBanner(
                id = "technology_digital",
                categoryName = "Technology & Digital",
                imageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=800&q=80",
            ),
        )
    }
}
