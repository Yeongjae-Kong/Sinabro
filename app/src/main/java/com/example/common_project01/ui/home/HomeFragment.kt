package com.example.common_project01.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Context
import android.content.CursorLoader
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.common_project01.R
import com.example.common_project01.databinding.FragmentHomeBinding
import com.example.common_project01.ui.DatabaseHelper
import com.example.common_project01.ui.UserProfile
import com.example.common_project01.ui.friends.RealPathUtil
import kotlin.properties.Delegates

object RealPathUtil {

    @SuppressLint("NewApi")
    fun getRealPathFromURI_API19(context: Context, uri: Uri): String {
        Log.d("myTag","19")
        var filePath = ""
        val wholeID = DocumentsContract.getDocumentId(uri)

        // Split at colon, use second item in the array
        val id = wholeID.split(":")[1]

        val column = arrayOf(MediaStore.Images.Media.DATA)

        // where id is equal to
        val sel = MediaStore.Images.Media._ID + "=?"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            column, sel, arrayOf(id), null
        )?.use { cursor ->
            val columnIndex = cursor.getColumnIndex(column[0])

            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex)
            }
        }

        return filePath
    }

    @SuppressLint("NewApi")
    fun getRealPathFromURI_API11to18(context: Context, contentUri: Uri): String? {
        Log.d("myTag","18")
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursorLoader = CursorLoader(
            context,
            contentUri, proj, null, null, null
        )
        val cursor = cursorLoader.loadInBackground()

        cursor?.use {
            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (cursor.moveToFirst()) {
                return cursor.getString(column_index)
            }
        }
        return null
    }

    fun getRealPathFromURI_BelowAPI11(context: Context, contentUri: Uri): String? {
        Log.d("myTag","11")
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        context.contentResolver.query(contentUri, proj, null, null, null)?.use { cursor ->
            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (cursor.moveToFirst()) {
                return cursor.getString(column_index)
            }
        }
        return null
    }
}
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var userID: String = "userID"
    private lateinit var profile: UserProfile
    private var currentYear: Int = 0
    private var currentMonth: Int = 0
    private var currentDay: Int = 0
    private lateinit var str: String
    private lateinit var diaryDatabaseHelper: DatabaseHelper
    private var userPrimaryKey by Delegates.notNull<Int>()
    private lateinit var user:UserProfile

    companion object {
        const val IMAGE_REQUEST_CODE = 1000
    }

    // onCreateView: Fragment의 뷰를 생성할 때 호출하는 메서드
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View{

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root
        diaryDatabaseHelper = DatabaseHelper(requireContext())

        val tmpUserPrimaryKey = arguments?.getInt("userPrimaryKey")
        profile = diaryDatabaseHelper.getProfile()[0]

        userPrimaryKey = tmpUserPrimaryKey ?: profile.primaryKey

        user = diaryDatabaseHelper.getUser(userPrimaryKey)!!
        userID = user.id
        Log.d("myTag",userPrimaryKey.toString())

        // 현재 날짜로 초기화
        val calendar = Calendar.getInstance()
        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH)
        currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val databaseHelper = DatabaseHelper(requireContext())
        val userID = databaseHelper.getProfileUserId() ?: "UserId"

        // 초기 날짜로 일기 데이터 로드
        loadDiaryData(currentYear, currentMonth, currentDay, userID)

        // UI 초기화
        with(binding) {

            title.text = user?.id.toString()
            Log.d("myTag",user?.name.toString())
            // CalendarView의 날짜 변경 리스너 설정
            calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
                currentYear = year
                currentMonth = month
                currentDay = dayOfMonth
                Log.d("myTag","힘드됴")
                loadDiaryData(year, month, dayOfMonth, userID)
            }
            if(user.profile) {
                mineButton.text = "하루 기록 추가하기"
            }else{

                mineButton.text = "남들의 시나브로 보기"
            }
            if(!user.profile){
                saveBtn.visibility = View.INVISIBLE
                updateBtn.visibility = View.INVISIBLE
                deleteBtn.visibility = View.INVISIBLE
            }
                saveBtn.setOnClickListener {
                    val date = formatDate(currentYear, currentMonth, currentDay)
                    val content = contextEditText.text.toString()
                    val diaryData = user?.let { it1 -> diaryDatabaseHelper.getDiary(date, it1.id) }
                    val imageUri = diaryData?.image ?: ""
                    diaryDatabaseHelper.insertOrUpdateDiary(userID, date, imageUri, content)
                    contextEditText.visibility = View.INVISIBLE
                    saveBtn.visibility = View.INVISIBLE
                    updateBtn.visibility = View.VISIBLE
                    deleteBtn.visibility = View.VISIBLE
                    str = contextEditText.text.toString()
                    diaryContent.text = str
                    diaryContent.visibility = View.VISIBLE
                }
                updateBtn.setOnClickListener {
                    contextEditText.visibility = View.VISIBLE
                    diaryContent.visibility = View.INVISIBLE
                    str = contextEditText.text.toString()
                    contextEditText.setText(str)
                    saveBtn.visibility = View.VISIBLE
                    updateBtn.visibility = View.INVISIBLE
                    deleteBtn.visibility = View.INVISIBLE
                    diaryContent.text = contextEditText.text
                }
                deleteBtn.setOnClickListener {
                    val date = formatDate(currentYear, currentMonth, currentDay)
                    diaryDatabaseHelper.deleteDiary(date)
                    diaryContent.visibility = View.INVISIBLE
                    updateBtn.visibility = View.INVISIBLE
                    deleteBtn.visibility = View.INVISIBLE
                    imageView.visibility = View.INVISIBLE
                    pickImageButton.visibility = View.VISIBLE
                    contextEditText.setText("")
                    contextEditText.visibility = View.VISIBLE
                    saveBtn.visibility = View.VISIBLE
                }
                pickImageButton.setOnClickListener {
                    pickImageFromGallery()
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.pickImageButton.visibility = View.INVISIBLE
                    }, 2000)
                }
                mineButton.setOnClickListener{
                    if(user.profile){
                        mineButton
                    }else{
                        val bundle = Bundle()
                        bundle.putInt("userPrimaryKey", profile.primaryKey) // 전달할 데이터
                        findNavController().navigate(R.id.navigation_home, bundle)
                    }

                }
        }

        return view
    }

    private fun formatDate(year: Int, month: Int, day: Int): String {
        return "$year-${month + 1}-$day"
    }

    private fun loadDiaryData(year: Int, month: Int, day: Int, userID: String) {
        val selectedDate = formatDate(year, month, day)
        val diaryData = diaryDatabaseHelper.getDiary(selectedDate,userID)


        Log.d("myTag",user.primaryKey.toString()+profile.primaryKey.toString())
        if(user.primaryKey!=profile.primaryKey){
            Log.d("myTag","우리 다르긴 하져?")
            binding.saveBtn.visibility = View.INVISIBLE
            binding.updateBtn.visibility = View.INVISIBLE
            binding.deleteBtn.visibility = View.INVISIBLE
            if (diaryData != null && diaryData.date == selectedDate && diaryData.userId == user.id) {
                // diaryData가 존재하고, 선택된 날짜와 일치하는 경우 데이터 로딩
                binding.diaryContent.setText(diaryData.feed)
                binding.imageView.setImageURI(Uri.parse(diaryData.image))
                binding.contextEditText.visibility = View.INVISIBLE
                binding.diaryContent.visibility = View.VISIBLE
                binding.pickImageButton.visibility = View.INVISIBLE
            } else {
                // diaryData가 null이거나 선택된 날짜와 일치하지 않는 경우
                clearUI()
                binding.contextEditText.setText("")
                binding.pickImageButton.visibility = View.INVISIBLE
                binding.contextEditText.visibility = View.INVISIBLE
                binding.diaryContent.visibility = View.INVISIBLE  // diaryContent를 INVISIBLE로 설정
            }
        }else{
            if (diaryData != null && diaryData.date == selectedDate) {
                // diaryData가 존재하고, 선택된 날짜와 일치하는 경우 데이터 로딩
                binding.diaryContent.setText(diaryData.feed)
                binding.imageView.setImageURI(Uri.parse(diaryData.image))
                binding.pickImageButton.visibility = View.INVISIBLE
                // 이미지가 없으면 이미지 선택 버튼 보이게
                if (diaryData.image.isEmpty()&&(user.primaryKey==profile.primaryKey)) {
                    binding.pickImageButton.visibility = View.VISIBLE
                } else {
                    binding.pickImageButton.visibility = View.INVISIBLE
                }
                binding.contextEditText.visibility = View.INVISIBLE
                binding.diaryContent.visibility = View.VISIBLE
                binding.updateBtn.visibility = View.VISIBLE
                binding.deleteBtn.visibility = View.VISIBLE
                binding.saveBtn.visibility = View.INVISIBLE
            } else {
                // diaryData가 null이거나 선택된 날짜와 일치하지 않는 경우
                clearUI()
                binding.contextEditText.setText("")
                binding.contextEditText.visibility = View.VISIBLE
                binding.diaryContent.visibility = View.INVISIBLE  // diaryContent를 INVISIBLE로 설정
            }
        }


    }

    private fun clearUI() {
        with(binding) {
            contextEditText.setText("")
            imageView.setImageDrawable(null)
            pickImageButton.visibility = View.VISIBLE
            diaryContent.visibility = View.VISIBLE
            saveBtn.visibility = View.VISIBLE
            updateBtn.visibility = View.INVISIBLE
            deleteBtn.visibility = View.INVISIBLE
        }
    }
    // 이미지 선택을 위한 인텐트를 시작하는 메서드
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    // 이미지 선택 결과를 처리하는 메서드
    @SuppressLint("SuspiciousIndentation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val databaseHelper = DatabaseHelper(requireContext())
        val userID = databaseHelper.getProfileUserId() ?: "UserId"

        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
//
            var realPath: String? = null
            if (Build.VERSION.SDK_INT < 11)
                Log.d("myTag","1111")
                if (data != null) {
                    realPath = data.data?.let { RealPathUtil.getRealPathFromURI_BelowAPI11(requireContext(), it) }
                }
            else if (Build.VERSION.SDK_INT < 19)
                    Log.d("myTag","1199")
                if (data != null) {
                    realPath = data.data?.let { RealPathUtil.getRealPathFromURI_API11to18(requireContext(), it) }
                }
            else
                if (data != null) {
                    Log.d("myTag","elseelse")
                    realPath = data.data?.let { RealPathUtil.getRealPathFromURI_API19(requireContext(), it) }
                }
            data?.data?.toString()?.let { Log.d("myTag", it) }

            data?.data?.toString()?.let { Log.d("myTag", realPath.toString()) }
            binding.imageView.setImageURI(Uri.parse(realPath))

            val date = formatDate(currentYear, currentMonth, currentDay)
            val diaryData = diaryDatabaseHelper.getDiary(date,user.id)

            if (diaryData != null && diaryData.date == date) {
                // diaryData가 존재하고, 선택된 날짜와 일치하는 경우 데이터 로딩
                binding.diaryContent.text = diaryData.feed
            }
            var feed = ""
            if (diaryData != null) {
                feed = diaryData.feed
            }
            diaryDatabaseHelper.insertOrUpdateDiary(userID, date, realPath.toString(), feed)
//
//            data?.data?.let { imageUri ->
//                binding.imageView.setImageURI(imageUri)
//
//                val date = formatDate(currentYear, currentMonth, currentDay)
//                val diaryData = diaryDatabaseHelper.getDiary(date,user.id)
//
//                if (diaryData != null && diaryData.date == date) {
//                    // diaryData가 존재하고, 선택된 날짜와 일치하는 경우 데이터 로딩
//                    binding.diaryContent.text = diaryData.feed
//                }
//                var feed = ""
//                if (diaryData != null) {
//                    feed = diaryData.feed
//                }
//                diaryDatabaseHelper.insertOrUpdateDiary(userID, date, imageUri.toString(), feed)
//            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}