package com.example.campusfix

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CampusFixApp()
            }
        }
    }
}

// Data model for Campus Issues
data class Issue(
    val id: String = "",
    val userId: String = "",
    val studentName: String = "",
    val rollNumber: String = "",
    val department: String = "",
    val course: String = "",
    val hostelLocation: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val status: String = "Pending", // Pending, In Progress, Resolved
    val timestamp: Long = 0L,
    val imageUrl: String = "",
    val upvotedUserIds: List<String> = emptyList(),
    val upvotes: Int = 0,
    val historyLog: List<String> = emptyList()
)

@Composable
fun CampusFixApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var userRole by remember { mutableStateOf<String?>(null) }
    var isProfileComplete by remember { mutableStateOf(true) }
    var isCheckingData by remember { mutableStateOf(auth.currentUser != null) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            isCheckingData = true
            firestore.collection("Users").document(currentUser!!.uid).get()
                .addOnSuccessListener { doc ->
                    userRole = doc.getString("role") ?: "Student"
                    val fullName = doc.getString("fullName") ?: ""
                    val rollNum = doc.getString("rollNumber") ?: ""
                    val course = doc.getString("course") ?: ""
                    val hostel = doc.getString("hostel") ?: ""
                    val roomNo = doc.getString("roomNo") ?: ""
                    val isCompleteFlag = doc.getBoolean("profileCompleted") ?: false

                    isProfileComplete = userRole == "Admin" || isCompleteFlag || (fullName.isNotBlank() && rollNum.isNotBlank() && (hostel.isNotBlank() || course.isNotBlank() || roomNo.isNotBlank()))
                    isCheckingData = false
                }
                .addOnFailureListener {
                    userRole = "Student"
                    isCheckingData = false
                }
        } else {
            userRole = null
            isCheckingData = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF4F7F6)
    ) {
        if (currentUser != null) {
            if (isCheckingData) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF176B55))
                }
            } else if (!isProfileComplete) {
                ProfileSetupScreen(
                    currentUser = currentUser!!,
                    firestore = firestore,
                    onProfileSaved = { isProfileComplete = true },
                    onBack = {
                        auth.signOut()
                        currentUser = null
                        userRole = null
                    }
                )
            } else if (userRole == "Admin") {
                AdminDashboard(
                    onLogout = {
                        auth.signOut()
                        currentUser = null
                        userRole = null
                    }
                )
            } else {
                StudentDashboard(
                    currentUser = currentUser!!,
                    firestore = firestore,
                    onLogout = {
                        auth.signOut()
                        currentUser = null
                        userRole = null
                    }
                )
            }
        } else {
            AuthScreen(
                auth = auth,
                firestore = firestore,
                onLoginSuccess = { user -> currentUser = user }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    currentUser: FirebaseUser,
    firestore: FirebaseFirestore,
    onProfileSaved: () -> Unit,
    onBack: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("BTech") }
    var department by remember { mutableStateOf("CSE") }
    var gender by remember { mutableStateOf("Male") }
    var hostel by remember { mutableStateOf("") }
    var roomNo by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    var courseExpanded by remember { mutableStateOf(false) }
    var deptExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var hostelExpanded by remember { mutableStateOf(false) }

    val courseOptions = listOf("BTech", "MTech", "PhD")
    val deptOptions = listOf("CSE", "AIML", "ECE", "EEE", "ME", "Civil", "Other")
    val genderOptions = listOf("Male", "Female")
    val hostelOptions = if (gender == "Male") listOf("BH1", "BH2", "OH1", "OH2") else listOf("GH1", "GH2", "GH3", "GH4", "GH5")

    LaunchedEffect(currentUser.uid) {
        firestore.collection("Users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                doc.getString("fullName")?.takeIf { it.isNotBlank() }?.let { fullName = it }
                doc.getString("rollNumber")?.takeIf { it.isNotBlank() }?.let { rollNumber = it }
                doc.getString("course")?.takeIf { it.isNotBlank() }?.let { course = it }
                doc.getString("department")?.takeIf { it.isNotBlank() }?.let { department = it }
                doc.getString("gender")?.takeIf { it.isNotBlank() }?.let { gender = it }
                doc.getString("hostel")?.takeIf { it.isNotBlank() }?.let { hostel = it }
                doc.getString("roomNo")?.takeIf { it.isNotBlank() }?.let { roomNo = it }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Login")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFF176B55))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Complete Your Profile", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF176B55))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Please provide your campus details to continue.", color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(value = fullName, onValueChange = { fullName = it; errorMessage = "" }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = rollNumber, onValueChange = { rollNumber = it; errorMessage = "" }, label = { Text("Roll Number / Student ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(expanded = courseExpanded, onExpandedChange = { courseExpanded = !courseExpanded }, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = course, onValueChange = {}, readOnly = true, label = { Text("Course") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExpanded) }, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                ExposedDropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
                    courseOptions.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { course = option; courseExpanded = false }) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(expanded = deptExpanded, onExpandedChange = { deptExpanded = !deptExpanded }, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = department, onValueChange = {}, readOnly = true, label = { Text("Department") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) }, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                ExposedDropdownMenu(expanded = deptExpanded, onDismissRequest = { deptExpanded = false }) {
                    deptOptions.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { department = option; deptExpanded = false }) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = !genderExpanded }, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = gender, onValueChange = {}, readOnly = true, label = { Text("Gender") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) }, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                    genderOptions.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            gender = option; genderExpanded = false
                            if (gender == "Male" && !listOf("BH1", "BH2", "OH1", "OH2").contains(hostel)) hostel = ""
                            else if (gender == "Female" && !listOf("GH1", "GH2", "GH3", "GH4", "GH5").contains(hostel)) hostel = ""
                        })
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(expanded = hostelExpanded, onExpandedChange = { hostelExpanded = !hostelExpanded }, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = hostel, onValueChange = {}, readOnly = true, label = { Text("Hostel") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hostelExpanded) }, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                ExposedDropdownMenu(expanded = hostelExpanded, onDismissRequest = { hostelExpanded = false }) {
                    hostelOptions.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { hostel = option; hostelExpanded = false }) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = roomNo,
                onValueChange = { input -> if (input.all { it.isDigit() }) roomNo = input },
                label = { Text("Room No. (Numbers only)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    if (fullName.isBlank() || rollNumber.isBlank() || hostel.isBlank() || roomNo.isBlank()) {
                        errorMessage = "Please fill in all required profile fields."
                        return@Button
                    }
                    isSaving = true
                    val updates = mapOf(
                        "fullName" to fullName.trim(),
                        "rollNumber" to rollNumber.trim(),
                        "course" to course,
                        "department" to department,
                        "gender" to gender,
                        "hostel" to hostel,
                        "roomNo" to roomNo.trim(),
                        "profileCompleted" to true
                    )
                    firestore.collection("Users").document(currentUser.uid).set(updates, SetOptions.merge())
                        .addOnSuccessListener { isSaving = false; onProfileSaved() }
                        .addOnFailureListener { e -> isSaving = false; errorMessage = "Failed: ${e.message}" }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF176B55))
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("Save and Continue")
            }
        }
    }
}

@Composable
fun AuthScreen(auth: FirebaseAuth, firestore: FirebaseFirestore, onLoginSuccess: (FirebaseUser) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("CampusFix", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFF176B55))
        Spacer(modifier = Modifier.height(8.dp))
        Text("A better campus starts with you.", color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Text(if (isLogin) "Welcome Back" else "Create Account", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(value = email, onValueChange = { email = it; message = "" }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it; message = "" }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)

        if (isLogin) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = {
                    if (email.isBlank()) {
                        message = "Please enter your email address above first."
                        return@TextButton
                    }
                    isLoading = true
                    message = ""
                    auth.sendPasswordResetEmail(email.trim()).addOnCompleteListener { task ->
                        isLoading = false
                        message = if (task.isSuccessful) {
                            "Password reset email sent! Check your inbox."
                        } else {
                            task.exception?.localizedMessage ?: "Failed to send reset email."
                        }
                    }
                }) {
                    Text("Forgot Password?", color = Color(0xFF176B55), fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (message.isNotEmpty()) {
            Text(
                text = message,
                color = if (message.contains("sent") || message.contains("created")) Color(0xFF176B55) else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) { message = "Enter email and password."; return@Button }
                if (password.length < 6) { message = "Password must be at least 6 characters."; return@Button }
                isLoading = true; message = ""

                if (isLogin) {
                    auth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
                        isLoading = false
                        message = if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user?.isEmailVerified == true) {
                                onLoginSuccess(user)
                                ""
                            } else {
                                auth.signOut()
                                "Please verify your email first."
                            }
                        } else {
                            task.exception?.localizedMessage ?: "Login failed."
                        }
                    }
                } else {
                    auth.createUserWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val userData = hashMapOf("email" to email.trim(), "role" to "Student", "fullName" to "", "rollNumber" to "", "course" to "", "department" to "", "gender" to "", "hostel" to "", "roomNo" to "", "createdAt" to System.currentTimeMillis())
                            firestore.collection("Users").document(user!!.uid).set(userData)
                                .addOnSuccessListener {
                                    user.sendEmailVerification()
                                    auth.signOut()
                                    isLoading = false
                                    message = "Account created! Verify your email to login."
                                    isLogin = true
                                }
                                .addOnFailureListener { e -> isLoading = false; message = "Failed: ${e.message}" }
                        } else {
                            isLoading = false
                            val errorMsg = task.exception?.localizedMessage ?: "Registration failed."
                            message = if (errorMsg.contains("already in use", ignoreCase = true)) {
                                "This email is already registered. Please log in instead."
                            } else {
                                errorMsg
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF176B55))
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else Text(if (isLogin) "Login" else "Create Account")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { isLogin = !isLogin; message = "" }) {
            Text(if (isLogin) "New here? Create an account" else "Already registered? Login", color = Color(0xFF176B55))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(currentUser: FirebaseUser, firestore: FirebaseFirestore, onLogout: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val categoryOptions = listOf(
        "Plumbing and Water", "Electrical and Lighting", "Heating, Cooling, and Ventilation (HVAC)",
        "Furniture, Beds, and Lockers", "Wi-Fi and Internet", "Housekeeping and Cleanliness",
        "Laundry and Linen", "Pest Control", "Kitchen and Dining", "Noise and Curfew Violations",
        "Guest Conduct and Disputes", "Theft and Lost & Found", "Security and Room Access",
        "Fire and Health Safety", "Booking and Check-In", "Billing, Deposits, and Refunds", "Staff and Front Desk Service"
    )
    var category by remember { mutableStateOf(categoryOptions[0]) }
    var categoryExpanded by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    var issuesList by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var studentName by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var studentCourse by remember { mutableStateOf("") }
    var hostelLocation by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        firestore.collection("Users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                studentName = doc.getString("fullName") ?: "Student"
                rollNumber = doc.getString("rollNumber") ?: ""
                department = doc.getString("department") ?: ""
                studentCourse = doc.getString("course") ?: "BTech"
                val hostel = doc.getString("hostel") ?: ""
                val room = doc.getString("roomNo") ?: ""
                hostelLocation = "$hostel, Room $room"
            }
    }

    DisposableEffect(studentCourse) {
        if (studentCourse.isNotBlank()) {
            val listener = firestore.collection("Issues")
                .whereEqualTo("course", studentCourse)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("StudentDashboard", "Listen failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        issuesList = snapshot.toObjects(Issue::class.java).sortedByDescending { it.timestamp }
                    }
                }
            onDispose { listener.remove() }
        } else {
            onDispose { }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CampusFix - $studentCourse Portal") },
                actions = { IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = Color(0xFF176B55)) {
                Icon(Icons.Filled.Add, contentDescription = "Report", tint = Color.White)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (issuesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No $studentCourse issues reported yet. Tap '+' to create one.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(issuesList) { issue ->
                        IssueCard(
                            issue = issue,
                            isAdmin = false,
                            currentUserId = currentUser.uid,
                            firestore = firestore,
                            onStatusChange = {}
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Report Campus Issue ($studentCourse)") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Issue Title") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))

                            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }, modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(value = category, onValueChange = {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                                    categoryOptions.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { category = option; categoryExpanded = false }) }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(onClick = { imageLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (selectedImageUri == null) "Attach Photo Evidence" else "Photo Attached ✅")
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (title.isNotBlank() && description.isNotBlank()) {
                                    isUploadingImage = true
                                    val newIssueId = firestore.collection("Issues").document().id
                                    val timestamp = System.currentTimeMillis()
                                    val initialLog = "Created by $studentName at ${SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(timestamp))}"

                                    fun saveIssueToFirestore(imgUrl: String) {
                                        val issue = Issue(
                                            id = newIssueId,
                                            userId = currentUser.uid,
                                            studentName = studentName,
                                            rollNumber = rollNumber,
                                            department = department,
                                            course = studentCourse,
                                            hostelLocation = hostelLocation,
                                            title = title,
                                            description = description,
                                            category = category,
                                            status = "Pending",
                                            timestamp = timestamp,
                                            imageUrl = imgUrl,
                                            upvotedUserIds = listOf(currentUser.uid),
                                            upvotes = 1,
                                            historyLog = listOf(initialLog)
                                        )
                                        firestore.collection("Issues").document(newIssueId).set(issue)
                                            .addOnSuccessListener {
                                                isUploadingImage = false
                                                title = ""
                                                description = ""
                                                selectedImageUri = null
                                                showDialog = false
                                            }
                                            .addOnFailureListener { e ->
                                                isUploadingImage = false
                                                Log.e("FirestoreSave", "Failed to save issue: ${e.message}", e)
                                            }
                                    }

                                    if (selectedImageUri != null) {
                                        val storageRef = FirebaseStorage.getInstance().reference.child("issue_images/$newIssueId.jpg")
                                        storageRef.putFile(selectedImageUri!!)
                                            .addOnSuccessListener {
                                                storageRef.downloadUrl.addOnSuccessListener { downloadUri: Uri ->
                                                    saveIssueToFirestore(downloadUri.toString())
                                                }.addOnFailureListener { e ->
                                                    Log.e("StorageUpload", "Failed to get download URL: ${e.message}", e)
                                                    isUploadingImage = false
                                                    saveIssueToFirestore("")
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("StorageUpload", "Failed to upload image: ${e.message}", e)
                                                isUploadingImage = false
                                                saveIssueToFirestore("")
                                            }
                                    } else {
                                        saveIssueToFirestore("")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF176B55)),
                            enabled = !isUploadingImage
                        ) {
                            if (isUploadingImage) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            else Text("Submit")
                        }
                    },
                    dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(onLogout: () -> Unit) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var allIssues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }

    DisposableEffect(Unit) {
        val listener = firestore.collection("Issues").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AdminDashboard", "Listen failed", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                allIssues = snapshot.toObjects(Issue::class.java).sortedByDescending { it.timestamp }
            }
        }
        onDispose { listener.remove() }
    }

    val filteredIssues = when (selectedFilter) {
        "Pending" -> allIssues.filter { it.status == "Pending" }
        "In Progress" -> allIssues.filter { it.status == "In Progress" }
        "Resolved" -> allIssues.filter { it.status == "Resolved" }
        else -> allIssues
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CampusFix - Admin Dashboard") },
                actions = { IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            PrimaryScrollableTabRow(
                selectedTabIndex = when (selectedFilter) { "Pending" -> 1; "In Progress" -> 2; "Resolved" -> 3; else -> 0 },
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("All", "Pending", "In Progress", "Resolved").forEach { filter ->
                    Tab(selected = selectedFilter == filter, onClick = { selectedFilter = filter }, text = { Text(filter) })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                if (filteredIssues.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No issues found for '$selectedFilter'.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        items(filteredIssues) { issue ->
                            IssueCard(
                                issue = issue,
                                isAdmin = true,
                                currentUserId = "",
                                firestore = firestore,
                                onStatusChange = { newStatus ->
                                    val timeStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date())
                                    val updatedLog = issue.historyLog + "Status changed to '$newStatus' at $timeStr"
                                    firestore.collection("Issues").document(issue.id)
                                        .update(mapOf("status" to newStatus, "historyLog" to updatedLog))
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IssueCard(issue: Issue, isAdmin: Boolean, currentUserId: String, firestore: FirebaseFirestore, onStatusChange: (String) -> Unit) {
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    val hasUpvoted = issue.upvotedUserIds.contains(currentUserId)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = issue.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Surface(
                    color = when (issue.status) {
                        "Resolved" -> Color(0xFFD4EDDA)
                        "In Progress" -> Color(0xFFFFF3CD)
                        else -> Color(0xFFF8D7DA)
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(text = issue.status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = issue.description, color = Color.DarkGray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Category: ${issue.category} | Course: ${issue.course}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            if (issue.studentName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Reported by: ${issue.studentName} (${issue.rollNumber})", fontSize = 12.sp, color = Color(0xFF176B55), fontWeight = FontWeight.Medium)
                Text(text = "Location: ${issue.hostelLocation}", fontSize = 12.sp, color = Color.DarkGray)
            }

            // Photo Evidence Button (renders for both Student and Admin when imageUrl is present)
            if (issue.imageUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showImageDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0056b3))
                ) {
                    Text(text = "📷 Tap to View Attached Photo Evidence", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (!isAdmin) {
                    OutlinedButton(
                        onClick = {
                            if (!hasUpvoted && currentUserId.isNotBlank()) {
                                val newList = issue.upvotedUserIds + currentUserId
                                val timeStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date())
                                val newLog = issue.historyLog + "Upvoted ('Me Too') by student at $timeStr"
                                firestore.collection("Issues").document(issue.id)
                                    .update(mapOf("upvotedUserIds" to newList, "upvotes" to newList.size, "historyLog" to newLog))
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (hasUpvoted) Color(0xFF176B55) else Color.Gray)
                    ) {
                        Text(if (hasUpvoted) "✓ Me Too (${issue.upvotes})" else "Me Too (${issue.upvotes})", fontSize = 12.sp)
                    }
                } else {
                    Text(text = "Impact Count: ${issue.upvotes} students affected", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF176B55))
                }

                TextButton(onClick = { showHistoryDialog = true }) {
                    Text("View History Log", fontSize = 12.sp)
                }
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onStatusChange("Pending") }, modifier = Modifier.weight(1f)) { Text("Pending", fontSize = 11.sp) }
                    OutlinedButton(onClick = { onStatusChange("In Progress") }, modifier = Modifier.weight(1.2f)) { Text("In Progress", fontSize = 11.sp) }
                    Button(onClick = { onStatusChange("Resolved") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF176B55))) { Text("Resolved", fontSize = 11.sp) }
                }
            }
        }
    }

    // Image Preview Dialog (Accessible to both Student and Admin)
    if (showImageDialog && issue.imageUrl.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { showImageDialog = false },
            title = { Text("Photo Evidence") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = issue.imageUrl,
                        contentDescription = "Issue Photo Evidence",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showImageDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF176B55))
                ) {
                    Text("Close")
                }
            }
        )
    }

    // History Log Dialog
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Issue History Log") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    issue.historyLog.forEach { logEntry ->
                        Text(text = "• $logEntry", fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp), color = Color.DarkGray)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showHistoryDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF176B55))) {
                    Text("Close")
                }
            }
        )
    }
}