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
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation

import com.example.insta.R
import com.example.insta.api.MiRetrofitBuilder
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.fragment_register.*
import kotlinx.android.synthetic.main.fragment_register.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.util.*

class FragmentRegister : Fragment() {

    //variables estaticas privadas, codigo intent
    private companion object {
        const val PERMISSIONSTORAGE = 1
        const val GALLERYCODE = 1
    }
    //uri de la imagen que seleccionamos con el picker
    private var uriImage: Uri? = null
    //controller de los fragment
    private lateinit var navController: NavController
    //comprueba si el telefono introducido tiene un formato correcto
    private var phoneValid = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_register, container, false)
        //crear y añadir el adaptador al spinner
        ArrayAdapter.createFromResource(
            requireContext(), R.array.genres_array , R.layout.custom_spinner
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
        navController = Navigation.findNavController(view)
        //damos formato a los campos de texto y añadimos escucha al boton
        changePasswordField(btnVerPassword1, txtPasswordRegister)
        changePasswordField(btnVerPassword2, txtPasswordRegister2)

        //listener comprueba si el telefono es valido
        spinnerTelefono.setPhoneNumberValidityChangeListener {
            phoneValid = it
        }
        //añadimos escucha al campo de imagen para añadir una nueva imagen
        imagenRegister.setOnClickListener{
                checkAndRequestPermissions()
        }
        icono_add_photo_register.setOnClickListener{
                checkAndRequestPermissions()
        }
        //escucha alboton de eliminar imagen
        icono_remove_photo_register.setOnClickListener{
            //comprobamos por si se pulso por error
            val builder = AlertDialog.Builder(context)
            builder.setTitle(R.string.dialogTitleSure)
            builder.setMessage(R.string.dialogTxtDeletePhoto)
            builder.setPositiveButton(R.string.dialogOptionYes) { _: DialogInterface?, _: Int ->
                //en caso de que este seguro, se establece la imagen por defecto
                this.uriImage = null
                imagenRegister.setImageResource(R.drawable.user)
                imagenRegister.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
                icono_add_photo_register.visibility = View.VISIBLE
                icono_add_photo_register.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down)
                icono_remove_photo_register.visibility = View.GONE
            }
            //si no, no hacemos nada
            builder.setNegativeButton(R.string.dialogOptionNo){_: DialogInterface?, _: Int ->}
            builder.show()
        }
        //escucha al boton de registro
        btnRegistroRegister.setOnClickListener{
            //comprobamos que todos los campos son validos
            if(txtEmailRegister.text.isValid() && txtUsernameRegister.text.isValid() && txtNombreRegister.text.isValid() &&
                txtApellido1Register.text.isValid() && txtApellido2Register.text.isValid() && txtPasswordRegister.text.isValid() &&
                txtPasswordRegister2.text.isValid() && txtGeneroRegister.selectedItemPosition!=-1){
                //comprobamos si el telefono es valido
                if(phoneValid){
                    //por ultimo comprobamos el email tiene el formato correcto
                    if(!txtEmailRegister.text.isValidEmail()){
                        Toast.makeText(context, R.string.errorCampoEmail, Toast.LENGTH_SHORT).show()
                    } else{
                        //si las passwords coinciden
                        if(txtPasswordRegister.text.toString() == txtPasswordRegister2.text.toString()){
                            //iniciamos una corrutina en el hilo de IO(in-out)
                            CoroutineScope(IO).launch{
                                //desactivamos el boton para no seguir enviando peticiones
                                disableRegisterButton()
                                //mostramos informacion al usuario
                                showLoadingDialog()
                                //creamos la peticion y la enviamos encolandola al server
                                registerUser().enqueue(object : Callback<ResponseBody> {
                                    //en caso de error al conectar con el servidor
                                    override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
                                        setTxtError("Error al conectar con el servidor")
                                    }
                                    //si obtenemos respuesta del servidor
                                    override fun onResponse(call: Call<ResponseBody>?, response: Response<ResponseBody>?) {
                                        //en caso de que el recurso se haya creado
                                        if(response!!.code() == 201) {
                                            //mostramos informacion y volvemos al login
                                            showToast("Registro completado")
                                            FragmentLogin.registro = true
                                            gotoLoginFragment()
                                        }
                                        //si es otro codigo de error
                                        else {
                                            //reactivamos el boton
                                            enableRegisterButton()
                                            //ocultamos l ainformacion de cargando
                                            hideLoadingDialog()
                                            //dependiendo del error, mostramos mensaje al usuario
                                            val error = response.errorBody()!!.string().toString()
                                            when(error.length){
                                                111->{
                                                    showToast("Email y Usuario existentes")
                                                    setTxtError("Email y Usuario existentes")
                                                    showTxtError(txtEmailRegister)
                                                    showTxtError(txtUsernameRegister)
                                                }
                                                53->{
                                                    showToast("Email existente")
                                                    setTxtError("Email existente")
                                                    showTxtError(txtEmailRegister)
                                                }
                                                59->{
                                                    showToast("Usuario existente")
                                                    setTxtError("Usuario existente")
                                                    showTxtError(txtUsernameRegister)
                                                }
                                            }
                                        }
                                    }
                                })
                            }
                //mostramos informacion al usuario sobre los campos erroneos o faltantes
                        } else{
                            Toast.makeText(context, R.string.errorPasswordMatch, Toast.LENGTH_SHORT).show()
                            txtPasswordRegister.compoundDrawableTintList = context?.resources?.getColorStateList(R.color.errorRed, context?.theme)
                            txtPasswordRegister2.compoundDrawableTintList = context?.resources?.getColorStateList(R.color.errorRed, context?.theme)

                        }
                    }
                } else{
                    Toast.makeText(context, R.string.errorPhoneNumber, Toast.LENGTH_SHORT).show()
                    txtTelefonoRegister.compoundDrawableTintList = context?.resources?.getColorStateList(R.color.errorRed, context?.theme)
                }
            } else{
                Toast.makeText(context, R.string.errorCamposRellenos, Toast.LENGTH_SHORT).show()
            }
        }
    }

    //funciones para cambiar elementos de la UI dentro de corrutinas
    private fun showToast(text: String){
        CoroutineScope(Main).launch{
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
    private fun showLoadingDialog(){
        CoroutineScope(Main).launch { progressBarRegister.visibility = View.VISIBLE }
    }
    private fun hideLoadingDialog(){
        CoroutineScope(Main).launch { progressBarRegister.visibility = View.INVISIBLE }
    }
    private fun disableRegisterButton(){
        CoroutineScope(Main).launch { btnRegistroRegister.isEnabled = false }
    }
    private fun enableRegisterButton(){
        CoroutineScope(Main).launch { btnRegistroRegister.isEnabled = true }
    }
    private fun showTxtError(view: EditText){
        CoroutineScope(Main).launch { view.compoundDrawableTintList = context?.resources?.getColorStateList(R.color.errorRed, context?.theme) }
    }
    private fun setTxtError(txt: String){
        CoroutineScope(Main).launch { txtErrorRegistro.text= txt }
    }
    private fun gotoLoginFragment(){ // este metodo devuelve a la pantalla del login alojada en el backstack
        CoroutineScope(Main).launch {
            navController.popBackStack()
        }
    }

    //peticion al servidor con retrofit
    private fun registerUser(): Call<ResponseBody>{
        val generoArray = arrayOf('M', 'F', 'O')
        return MiRetrofitBuilder.apiService.register(
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
            getImageUpload()
        )
    }

    //metodo que transforma la url de la imagen en un fichero de imagen
    private fun getImageUpload() : MultipartBody.Part?{
        //comrpobamos si hay uri establecida (imagen seleccionada)
        if(this.uriImage != null) {
            //lo transformamos en bitmap, dependiendo de la version del cliente
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver, this.uriImage!!))
            } else {
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, this.uriImage)
            }
            //creamos el ficheo en formato jpg
            val file = File(requireContext().cacheDir, txtUsernameRegister.text.toString()+".jpg")
            file.createNewFile()
            //creamos el array de bytes para transformar el bitmap en array
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
            val bitmapdata = bos.toByteArray()
            val fos :FileOutputStream?
            //y lo escribimos en nuestro nuevo fichero
            try{
                fos = FileOutputStream(file)
                fos.write(bitmapdata)
                fos.flush()
                fos.close()
            } catch(e: FileNotFoundException){
                e.printStackTrace()
            } catch (e: IOException){
                e.printStackTrace()
            }
            //creamos el requestbody de la peticion que incluye nuestra imagen
            val requestFile = RequestBody.create(MediaType.parse(MimeTypeMap.getFileExtensionFromUrl(uriImage!!.toString())), file)
            //val requestFile = RequestBody.create(MediaType.parse("image/*"), file)
            //y lo devolvemos
            return MultipartBody.Part.createFormData("imagen", file.name, requestFile)
        }
        //en caso de que no haya imagen, creamos un recurso "vacio" y lo devolvemos
        val requestFile = RequestBody.create(MediaType.parse("text/plain"), "")
        return MultipartBody.Part.createFormData("imagen", null, requestFile)
    }


    //metodo para cambiar el formato de texto de los campos contraseña para hacerla visible o no
    private fun changePasswordField(checkBox: CheckBox, txt: EditText){
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                checkBox.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_clockwise)
                checkBox.buttonDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_visibility_off)
                txt.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_left)
                txt.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else{
                checkBox.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_anticlockwise)
                checkBox.buttonDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_visibility)
                txt.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_right)
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

    //metodo para abrir la galeria con un intent
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
                        imagenRegister.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
                        icono_add_photo_register.visibility = View.GONE
                        icono_remove_photo_register.visibility = View.VISIBLE
                        icono_remove_photo_register.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_in)
                    }
                } else if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                    Toast.makeText(activity, "Error al recortar la imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }


    }
    //metodo que abre la actividad que recorta las imagenes
    private fun launchImageCrop(uri: Uri){
        CropImage.activity(uri)
            .setGuidelines(CropImageView.Guidelines.ON_TOUCH)
            .setAspectRatio(1, 1)
            .setCropShape(CropImageView.CropShape.RECTANGLE)
            .start(requireContext(), this)
    }
    //comrpobamos si tenemos los permisos y si no los pedimos para abrir la galeria
    private fun checkAndRequestPermissions(){
        if(ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(activity as Activity ,android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                openGallery()
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
