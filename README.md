ExpenSEEs

Overview
ExpenSEEs is a mobile expense tracker application designed to help users manage their daily expenses efficiently. Developed over six weeks in 2025 by Andrew Emmanuel A. Abarientos (Frontend Developer) and Al Francis B. Paz (Backend Developer) during their internship at REVLV, this app is a milestone project for two 4th-year Computer Engineering students at Ateneo de Naga University. It provides intuitive features for tracking, categorizing, and visualizing expenses, empowering users to gain better insight into their spending habits.
Features

Expense Tracking: Add, delete, and view expenses with details such as category, amount, date, and remarks, with support for attaching receipt images.
Budget Management: Create and monitor budgets, including status tracking (Pending, Released, Denied, Liquidated).
Liquidation Reports: Generate and view detailed liquidation reports with receipt details, financial summaries, and status indicators.
User Authentication: Secure sign-in, refresh token, password reset, and forgot password functionalities.
Profile Management: Upload and retrieve user profile pictures.
Responsive UI: Built with Jetpack Compose for a modern, user-friendly interface with smooth navigation and animations.
Real-time Data: Fetches and displays data using Retrofit for seamless API integration.
Image Handling: Capture and store receipt images using a custom FileProvider for secure file access.


Tech Stack

Frontend: 
Jetpack Compose for UI
Material3 for consistent design
Android Navigation Component for screen navigation


Backend: 
Retrofit for API calls
OkHttp for HTTP client with logging
Gson for JSON serialization/deserialization


Utilities:
Custom FileProvider for generating secure URIs for receipt images


API: Communicates with a backend server at http://152.42.192.226:8080/ for data operations
Language: Kotlin
Minimum SDK: Android Oreo (API 26, Build.VERSION_CODES.O)

Installation

Clone the Repository:git clone https://github.com/your-repo/expensees.git


Open in Android Studio:
Open the project in Android Studio.
Ensure you have the latest Android SDK and Gradle installed.


Configure FileProvider:
Ensure the FileProvider is configured in the AndroidManifest.xml with the authority ${applicationId}.fileprovider.
Verify that the res/xml/file_paths.xml is set up to allow access to the cache directory for image storage.


Build and Run:
Sync the project with Gradle.
Run the app on an emulator or physical device running Android Oreo or higher.



Usage

Sign In: Use your credentials to sign in or use the "Forgot Password" feature to receive a reset link via email.
Track Expenses: Add expenses with details like category, amount, and optional receipt images. Delete expenses as needed.
Manage Budgets: Create budgets and view their status and associated expenses in the Budget Details screen.
View Liquidation Reports: Access detailed reports for submitted budgets, including total spent, remaining balance, and receipt details.
Navigate: Use the back and home buttons to move between screens, or refresh reports for updated data.
Handle Images: Capture receipt images, which are stored securely using a FileProvider and linked to expenses.

Project Structure

MainActivity.kt: Entry point, sets up Retrofit, OkHttp, and AuthRepository for API interactions.
AboutScreen.kt: Displays app and team information.
BudgetDetailsScreen.kt: Shows budget details and associated expenses.
DetailedLiquidationReport.kt: Presents detailed liquidation report data with receipt details, formatted dates, and financial summaries.
LiquidationReportsScreen.kt: Lists all liquidation reports with filtering by status (Pending, Denied, Liquidated).
ForgotPassword.kt: Provides a UI for users to request a password reset link via email.
AppNavigation.kt: Defines the navigation graph using Jetpack Compose Navigation, handling transitions between screens like Loading, Login, Home, Record Expenses, and more.
ApiService.kt: Defines API endpoints for authentication, expense management, budget operations, and liquidation reports.
ApiConfig.kt: Contains the base URL for API communication.
ImageUtils.kt: Utility for generating secure URIs for receipt images using FileProvider.
AuthRepository: Manages API calls and data storage for authentication and reports.

API Endpoints

Authentication:
POST /api/auth/sign-in: User login
POST /api/auth/refresh-token: Refresh access token
POST /api/forgotPassword/reset-password: Reset password


Expenses:
POST /api/expenses: Add an expense
DELETE /api/expenses/{expenseId}: Delete an expense
GET /api/expenses: Retrieve all expenses


Budgets:
POST /api/budgets: Create a budget
GET /api/budgets: Retrieve all budgets


Profile:
POST /api/users/{userId}/profile-picture: Upload profile picture
GET /api/users/{userId}/profile-picture: Retrieve profile picture


Liquidation Reports:
POST /api/liquidation: Submit a liquidation report
GET /api/liquidation: Retrieve all liquidation reports
GET /api/liquidation/{liquidationId}: Retrieve a specific liquidation report



Navigation
The app uses Jetpack Compose Navigation for seamless screen transitions:

Loading Screen: Initial screen that transitions to Login after loading.
Login Screen: Handles user authentication and redirects to Home or Forgot Password screens.
Home Screen: Central hub for accessing expense tracking, budget management, and logout functionality.
Record Expenses Screen: Allows users to input expense details and attach receipt images.
Expense List Screen: Displays and manages the list of user expenses with delete functionality.
Fund Request Screen: Facilitates budget creation.
Requested Budgets Screen: Lists all submitted budgets with their statuses.
Liquidation Report Screen: Displays details for a specific budget's liquidation report.
Detailed Liquidation Report Screen: Shows detailed expense data, including formatted dates, categories, remarks, and amounts.
Reset Password Screen: Allows users to reset their password after receiving a link.
Forgot Password Screen: Sends a password reset link to the user's email.
Notifications Screen: Displays app notifications.
About Screen: Provides information about the app and development team.

Key Components

Forgot Password: Users can request a password reset link by entering their email. The UI features a gradient button with loading states and error handling (e.g., "Email not found" for invalid emails).
Detailed Liquidation Report: Displays comprehensive report data with a formatted expense table, status indicators (color-coded dots), and financial summaries (total budgeted, spent, and remaining).
Image Handling: The ImageUtils.kt utility generates secure URIs for receipt images, stored in the app's cache directory, ensuring compatibility with Android's FileProvider.


Contact
For inquiries, contact:

Andrew Emmanuel A. Abarientos (Frontend Developer) : 09070357944
Al Francis B. Paz (Backend Developer) 09772153941

Thank you for using ExpenSEEs!
