package libjni.constants

// these are macros in C and cannot be generate by bindgen, so we define them manually
val JNI_OK        = 0    /* success */
val JNI_ERR       = (-1) /* unknown error */
val JNI_EDETACHED = (-2) /* thread detached from the VM */
val JNI_EVERSION  = (-3) /* JNI version error */
val JNI_ENOMEM    = (-4) /* not enough memory */
val JNI_EEXIST    = (-5) /* VM already created */
val JNI_EINVAL    = (-6) /* invalid arguments */

val JNI_VERSION_1_1 = 0x00010001
val JNI_VERSION_1_2 = 0x00010002
val JNI_VERSION_1_4 = 0x00010004
val JNI_VERSION_1_6 = 0x00010006
val JNI_VERSION_1_8 = 0x00010008
val JNI_VERSION_9   = 0x00090000
val JNI_VERSION_10  = 0x000a0000
val JNI_VERSION_19  = 0x00130000
val JNI_VERSION_20  = 0x00140000
val JNI_VERSION_21  = 0x00150000
