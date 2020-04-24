package com.example.insta.views.fragments

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.example.insta.R
import com.example.insta.api.MiRetrofitBuilder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.fragment_register.*
import kotlinx.android.synthetic.main.fragment_register.view.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import java.io.*
import java.util.*

class FragmentRegister : Fragment() {

    private companion object {
        const val PERMISSIONSTORAGE = 1
        const val GALLERYCODE = 1
    }
    private var uriImage: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lateinit var sad :String

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_register, container, false)
        //crear y añadir el adaptador al spinner
        ArrayAdapter.createFromResource(
            context!!, R.array.genres_array , R.layout.custom_spinner
        ).also { adapter->
            adapter.setDropDownViewResource(R.layout.custom_spinner)
            view.txtGeneroRegister.adapter = adapter
        }
        //añadimos una imagen por defecto al imageview de imagen de usuario
        view.imagenRegister.setImageResource(R.drawable.user)
        //instanciamos el calendar a la fecha actual
        view.txtFechaRegister.maxDate = Calendar.getInstance().timeInMillis
        //cambiamos el color del icono de los textview
        view.txtEmailRegister.compoundDrawableTintList = context?.resources?.getColorStateList(R.color.grisClaro, context?.theme)
        view.txtUsernameRegister.compoundDrawableTintList = context?.resources?.getColorStateList(R.color.grisClaro, context?.theme)
        view.txtNombreRegister.compoundDrawableTintList = context?.resources?.getColorStateList(R.color.grisClaro, context?.theme)
        view.txtApellido1Register.compoundDrawableTintList = context?.resources?.getColorStateList(R.color.grisClaro, context?.theme)
        view.txtApellido2Register.compoundDrawableTintList = context?.resources?.getColorStateList(R.color.grisClaro, context?.theme)
        view.txtPasswordRegister2.compoundDrawableTintList = context?.resources?.getColorStateList(R.color.grisClaro, context?.theme)
        view.txtPasswordRegister.compoundDrawableTintList = context?.resources?.getColorStateList(R.color.grisClaro, context?.theme)
        //estilizamos los campos de texto escuchando
        changeTextField(view.txtEmailRegister)
        changeTextField(view.txtUsernameRegister)
        changeTextField(view.txtNombreRegister)
        changeTextField(view.txtApellido1Register)
        changeTextField(view.txtApellido2Register)
        changeTextField(view.txtPasswordRegister2)
        changeTextField(view.txtPasswordRegister)
        view.spinnerTelefono.registerCarrierNumberEditText(view.txtTelefonoRegister)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //damos formato a los campos de texto y añadimos escucha al boton
        changePasswordField(btnVerPassword1, txtPasswordRegister)
        changePasswordField(btnVerPassword2, txtPasswordRegister2)

        spinnerTelefono.setPhoneNumberValidityChangeListener {
            if(it) Log.d("DEBUG", "HOAL")
            else Log.d("DEBUG", "HOAL")

        }
        //añadimos escucha al campo de imagen para añadir una nueva imagen
        imagenRegister.setOnClickListener{
            if(Build.VERSION.SDK_INT >= 22){
                checkAndRequestPermissions()
            } else{
                openGallery()
            }
        }
        icono_add_photo_register.setOnClickListener{
            if(Build.VERSION.SDK_INT >= 22){
                checkAndRequestPermissions()
            } else{
                openGallery()
            }
        }
        icono_remove_photo_register.setOnClickListener{
            val builder = AlertDialog.Builder(context)
            builder.setTitle(R.string.dialogTitleSure)
            builder.setMessage(R.string.dialogTxtDeletePhoto)
            builder.setPositiveButton(R.string.dialogOptionYes) { _: DialogInterface?, _: Int ->
                this.uriImage = null
                imagenRegister.setImageResource(R.drawable.user)
                imagenRegister.animation = AnimationUtils.loadAnimation(context!!, R.anim.fade_in)
                icono_add_photo_register.visibility = View.VISIBLE
                icono_add_photo_register.animation = AnimationUtils.loadAnimation(context!!, R.anim.slide_down)
                icono_remove_photo_register.visibility = View.GONE
            }
            builder.setNegativeButton(R.string.dialogOptionNo){_: DialogInterface?, _: Int ->}
            builder.show()

        }
        btnRegistroRegister.setOnClickListener{
            //TODO cuando coja mes del picker ++
            txtFechaRegister.year
            if(txtEmailRegister.text.isValid() && spinnerTelefono.fullNumberWithPlus.isValid() && txtUsernameRegister.text.isValid() && txtNombreRegister.text.isValid() &&
                txtApellido1Register.text.isValid() && txtApellido2Register.text.isValid() && txtPasswordRegister.text.isValid() &&
                txtPasswordRegister2.text.isValid() && txtGeneroRegister.selectedItemPosition!=-1){
                if(!txtEmailRegister.text.isValidEmail()){
                    Toast.makeText(context, R.string.errorCampoEmail, Toast.LENGTH_SHORT).show()
                } else{
                    if(txtPasswordRegister.text.toString() == txtPasswordRegister2.text.toString()){
                        val generoArray = arrayOf('M', 'F', 'O')
                        //Toast.makeText(context, txtGeneroRegister.selectedItemPosition.toString(), Toast.LENGTH_SHORT).show()
                        //Toast.makeText(context, spinnerTelefono.fullNumberWithPlus, Toast.LENGTH_SHORT).show()
                        //Toast.makeText(context, uriImage.toString(), Toast.LENGTH_SHORT).show()

                        MiRetrofitBuilder.apiService.register(
                            RequestBody.create(MediaType.parse("text/plain"), txtEmailRegister.text.toString()),
                            RequestBody.create(MediaType.parse("text/plain"), txtUsernameRegister.text.toString()),
                            RequestBody.create(MediaType.parse("text/plain"), txtNombreRegister.text.toString()),
                            RequestBody.create(MediaType.parse("text/plain"), txtApellido1Register.text.toString()),
                            RequestBody.create(MediaType.parse("text/plain"), txtApellido2Register.text.toString()),
                            RequestBody.create(MediaType.parse("text/plain"), "${txtFechaRegister.year}-${txtFechaRegister.month+1}-${txtFechaRegister.dayOfMonth}"),
                            RequestBody.create(MediaType.parse("text/plain"), spinnerTelefono.fullNumberWithPlus),
                            RequestBody.create(MediaType.parse("text/plain"), txtPasswordRegister.text.toString()),
                            RequestBody.create(MediaType.parse("text/plain"), txtPasswordRegister2.text.toString()),
                            RequestBody.create(MediaType.parse("text/plain"), generoArray[txtGeneroRegister.selectedItemPosition].toString()),
                            /*txtEmailRegister.text.toString(),
                            txtUsernameRegister.text.toString(),
                            txtNombreRegister.text.toString(),
                            txtApellido1Register.text.toString(),
                            txtApellido2Register.text.toString(),
                            "${txtFechaRegister.year}-${txtFechaRegister.month+1}-${txtFechaRegister.dayOfMonth}",
                            spinnerTelefono.fullNumberWithPlus,
                            txtPasswordRegister.text.toString(),
                            txtPasswordRegister2.text.toString(),
                            generoArray[txtGeneroRegister.selectedItemPosition].toString(),*/
                            getImageUpload()
                        ).enqueue(object : Callback<ResponseBody> {
                            override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {}

                            override fun onResponse(call: Call<ResponseBody>?, response: Response<ResponseBody>?) {
                                if(response!!.code() == 201) Log.d("retrofit", "es 201")
                                else Log.d("retrofit", generoArray[txtGeneroRegister.selectedItemPosition].toString())

                            }
                        })


                    } else{
                        Toast.makeText(context, R.string.errorPasswordMatch, Toast.LENGTH_SHORT).show()
                    }
                }
            } else{
                Toast.makeText(context, R.string.errorCamposRellenos, Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun getImageUpload() : MultipartBody.Part?{
        if(this.uriImage != null) {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context!!.contentResolver, this.uriImage!!))
            } else {
                MediaStore.Images.Media.getBitmap(context!!.contentResolver, this.uriImage)
            }
            val file = File(context!!.cacheDir, txtUsernameRegister.text.toString()+".jpg")
            file.createNewFile()
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
            val bitmapdata = bos.toByteArray()
            var fos :FileOutputStream? = null
            try{
                fos = FileOutputStream(file)
                fos.write(bitmapdata)
                fos.flush()
                fos.close()
                Log.d("retrofit", "FICHERO"+file.name)
                Log.d("retrofit", "FICHERO"+file.absoluteFile)
                Log.d("retrofit", "FICHERO"+file.path)
                Log.d("retrofit", "FICHERO"+file.extension)
                Log.d("retrofit", "FICHERO"+file.nameWithoutExtension)
            } catch(e: FileNotFoundException){
                e.printStackTrace()
            } catch (e: IOException){
                e.printStackTrace()
            }
            val requestFile = RequestBody.create(MediaType.parse(MimeTypeMap.getFileExtensionFromUrl(uriImage!!.toString())), file)
            //val requestFile = RequestBody.create(MediaType.parse("image/*"), file)
            return MultipartBody.Part.createFormData("imagen", file.name, requestFile)
        }
        val requestFile = RequestBody.create(MediaType.parse("text/plain"), "")
        return MultipartBody.Part.createFormData("imagen", null, requestFile)
    }


    //metodo para cambiar el formato de texto de los campos contraseña para hacerla visible o no
    private fun changePasswordField(checkBox: CheckBox, txt: EditText){
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                checkBox.animation = AnimationUtils.loadAnimation(context!!, R.anim.rotate_clockwise)
                checkBox.buttonDrawable = ContextCompat.getDrawable(context!!, R.drawable.ic_visibility_off)
                txt.animation = AnimationUtils.loadAnimation(context!!, R.anim.slide_in_left)
                txt.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else{
                checkBox.animation = AnimationUtils.loadAnimation(context!!, R.anim.rotate_anticlockwise)
                checkBox.buttonDrawable = ContextCompat.getDrawable(context!!, R.drawable.ic_visibility)
                txt.animation = AnimationUtils.loadAnimation(context!!, R.anim.slide_in_right)
                txt.transformationMethod = PasswordTransformationMethod.getInstance()

            }
        }
    }
    //metodo para comprobar el formato de email
    private fun CharSequence.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()
    private fun CharSequence.isValid() = !isNullOrEmpty()
    //metodo para capturar cada vez que se escriba un campo de texto para cambiar el color del icono
    private fun changeTextField(txt: EditText){
        txt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if(txt.text.isEmpty()){
                    txt.compoundDrawableTintList = context?.resources?.getColorStateList(R.color.grisClaro, context?.theme)
                } else{
                    txt.compoundDrawableTintList = context?.resources?.getColorStateList(R.color.azulClaro, context?.theme)
                }

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textEntered = txt.text.toString()

                if (textEntered.isNotEmpty() && textEntered.contains(" ")) {
                    txt.setText(txt.text.toString().replace(" ", ""))
                    txt.setSelection(txt.text!!.length)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                return
            }
        })
    }

    //metodo para abrir la galeria
    private fun openGallery(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent,
            GALLERYCODE
        )
    }
    //capturamos la respuesta de los intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            GALLERYCODE ->{ //capturamos la respuesta del intent que abre la galeria
                if(resultCode == RESULT_OK && data != null){
                    data.data?.let { uri->
                        launchImageCrop(uri)
                    }
                }
            }
            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE->{ //captura intent recortar imagen
                val result = CropImage.getActivityResult(data)
                if(resultCode == RESULT_OK){
                    result.uri?.let {uriImage->
                        this.uriImage = uriImage
                        imagenRegister.setImageURI(uriImage)
                        imagenRegister.animation = AnimationUtils.loadAnimation(context!!, R.anim.fade_in)
                        icono_add_photo_register.visibility = View.GONE
                        icono_remove_photo_register.visibility = View.VISIBLE
                        icono_remove_photo_register.animation = AnimationUtils.loadAnimation(context!!, R.anim.slide_up_in)
                    }
                } else if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                    Toast.makeText(activity, "Error al recortar la imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }


    }

    private fun launchImageCrop(uri: Uri){
        CropImage.activity(uri)
            .setGuidelines(CropImageView.Guidelines.ON_TOUCH)
            .setAspectRatio(1, 1)
            .setCropShape(CropImageView.CropShape.RECTANGLE)
            .start(context!!, this)
    }

    private fun checkAndRequestPermissions(){
        if(ContextCompat.checkSelfPermission(context!!, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(activity as Activity ,android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                Toast.makeText(activity, "holaaaaa", Toast.LENGTH_LONG).show()
            } else{
                ActivityCompat.requestPermissions(activity as Activity, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONSTORAGE
                )
            }
        } else{
            openGallery()
        }
    }



}
