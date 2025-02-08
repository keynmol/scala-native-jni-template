import libjni.all.*, libjni.constants.*
import scalanative.unsafe.*

@main def hello =
  Zone:
    // Part 1: initialising basic JNI interface
    val iface = libjni.structs.JNIInvokeInterface_()
    val args  = JavaVMInitArgs()
    (!args).version = jint(0x00010008) // JNI_VERSION_1_8

    val customClasspath =
      sys.env
        .get("CLASSPATH")
        .map(_.trim)

    customClasspath match
      case None =>
        (!args).nOptions = jint(0)
      case Some(value) =>
        val cp = "-Djava.class.path=" + value
        (!args).options = JavaVMOption(toCString(cp), null)
        (!args).nOptions = jint(1)

    val vm  = doublePointer(JavaVM(iface))
    val env = doublePointer[JNIEnv](JNIEnv(null))

    val res = JNI_CreateJavaVM(
      vm,
      env.asInstanceOf[Ptr[Ptr[Byte]]],
      args.asInstanceOf[Ptr[Byte]],
    )
    if res.value != JNI_OK then sys.error("Failed to create Java VMn")

    // look at this shit
    val jvm: JNINativeInterface_ = (!(!(!env)).value)

    // Part 2: using JNI interface to invoke built-in Java methods. Cann you guess which ones?
    val system = jvm.FindClass(!env, c"java/lang/System")
    assert(system.value != null, "Failed to find java.lang.System class")

    val outField =
      jvm.GetStaticFieldID(!env, system, c"out", c"Ljava/io/PrintStream;");
    assert(outField.value != null, "Failed to find .out field on System")

    val out = jvm.GetStaticObjectField(!env, system, outField)
    assert(out.value != null)

    val printStream = jvm.GetObjectClass(!env, out)
    assert(printStream.value != null)

    val printlnMethod =
      jvm.GetMethodID(!env, printStream, c"println", c"(Ljava/lang/String;)V")
    assert(printlnMethod.value != null)

    val str =
      jvm.NewStringUTF(!env, c"Hello world from Java from... Scala Native?..")

    val arguments = va_list(toCVarArgList(str))

    jvm.CallVoidMethodV(!env, out, printlnMethod, arguments)

    // This part will only work if the CLASSPATH env variable is set
    // to a classpath containing scala standard library jars
    // The build in this template does so automatically
    val scalaPredef = jvm.FindClass(!env, c"scala/Predef")
    assert(scalaPredef.value != null)

    val otherHello = jvm.NewStringUTF(
      !env,
      c"Hello world from Scala from JNI from Scala Native?...",
    )

    val scalaPrintlnMethod = jvm.GetStaticMethodID(
      !env,
      scalaPredef,
      c"println",
      c"(Ljava/lang/Object;)V",
    )
    assert(scalaPrintlnMethod.value != null)

    jvm.CallStaticObjectMethodV(
      !env,
      scalaPredef,
      scalaPrintlnMethod,
      toCVarArgList(otherHello),
    )

inline def doublePointer[A: Tag](value: A): Ptr[Ptr[A]] =
  val ptr1 = stackalloc[A]()
  val ptr2 = stackalloc[Ptr[A]]()
  ptr2.update(0, ptr1)(using Tag.materializePtrTag[A])
  !ptr1 = value

  ptr2
