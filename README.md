# E-Press – Kotlin News App

E-Press is an Android News Aggregator built using Kotlin, Jetpack Compose, and the GNews API. It allows you to explore the top UK headlines by category, bookmark favourite articles, toggle dark mode, view quick summaries in a dialog, and share news.

---

## How to Build and Run the Application

### Requirements
- Android Studio Meerkat | 2024.3.1 Patch 1
- Kotlin 2.0.21
- Gradle Plugin 8.9.1
- Minimum SDK: 24
- Target SDK: 35

### Instructions

1. **Clone the repository**
   Download the repository as a zip and uncompress it

3. **Open in Android Studio.**

4. **Set up your API key:**
- Open NewsViewModel and replace the apiKey variable with your own GNews API key.
- Alternatively, if you can get it to work, store it securely in local.properties and inject it via Gradle for production use.

4. **Click 'Run' or use the emulator/device.**

---

## Libraries & SDKs Used

### Library	Purpose
| Library                                 | Purpose                                   |
|-----------------------------------------|-------------------------------------------|
| Retrofit                                | For HTTP networking and GNews API calls   |
| OkHttp                                  | HTTP client with logging interceptor      |
| Retrofit Converter Gson                 | Converts JSON into Kotlin objects         |
| OkHttp Logging                          | Logs network calls for debugging          |
| Coil Compose                            | Efficient image loading in Compose        |
| Accompanist Pager                       | Horizontal tab swiping                    |
| Accompanist SwipeRefresh                | Pull-to-refresh behavior                  |
| Jetpack Compose Material3               | Modern UI components                      |
| ViewModel Compose                       | Compose integration for ViewModels        |
| Activity Compose                        | Entry point for Compose activities        |
| AndroidX ViewModel & Lifecycle          | State management and lifecycle awareness  |
| Core KTX                                | Kotlin extensions for Android framework   |
| Compose BOM                             | Compatible versions of Compose libraries  |
| Compose UI/Graphics/Tooling             | UI building blocks and dev tools          |
| Lifecycle Runtime KTX                   | Lifecycle-aware components                |
| Material Icons Extended                 | Extended Material icons for Compose       |
| JUnit / AndroidX JUnit / Espresso       | Testing frameworks                        |

All versions are also declared in libs.versions.toml

---

## Known Issues / Limitations
- Hardcoded API key in NewsViewModel for testing; not suitable for production.
- Offline mode is limited to bookmarked articles only — no full caching of previous API responses.
- No pagination or infinite scroll — only a fixed set of results are fetched per category.
- UI may not fully support tablet layouts or landscape mode.
- Search functionality is not implemented.
