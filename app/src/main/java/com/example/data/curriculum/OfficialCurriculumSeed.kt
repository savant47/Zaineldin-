package com.example.data.curriculum

import com.example.data.model.CurriculumLesson
import com.example.data.model.SubjectType

/**
 * قاعدة البيانات المرجعية الشاملة (Official Grounding Database)
 * مستخرجة مباشرة من الكتب الرسمية المرفوعة للصف الثاني الابتدائي (مدارس اللغات - المنهج المصري 2026/2027):
 * 1. كتاب وزارة التربية والتعليم + ملحق الأضواء في (اللغة العربية) - الصف الثاني الابتدائي الترم الأول 2026/2027
 * 2. كتاب وزارة التربية والتعليم والأزهر الشريف في (التربية الدينية الإسلامية) - الصف الثاني الابتدائي 2026/2027
 * 3. كتاب وزارة التربية والتعليم (Primary 2 Mathematics Term 1 2026/2027 - المنهج المصري الياباني + كتاب التقييمات والمراجعة الشاملة AlTaasis AlSaleem)
 * 4. كتاب وزارة التربية والتعليم (Primary 2 English Term 1 2026/2027 - Units 1 to 6)
 * 5. دفتر تحضير وشرح الدروس المتقدم (Mr Sobhy Borda - English Language Lesson Preparation Notebook Primary Two - Units 1 to 6)
 */
object OfficialCurriculumSeed {

    fun getInitialLessons(userId: String): List<Pair<String, CurriculumLesson>> {
        return listOf(
            // =========================================================================
            // ١. اللغة العربية (الكتاب المدرسي الرسمي ٢٠٢٦/٢٠٢٧ + تأسيس ومراجعات الأضواء)
            // =========================================================================
            "arabic_foundation_phonics" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.ARABIC.id,
                subjectNameAr = SubjectType.ARABIC.titleAr,
                unitTitle = "تأسيس ومراجعة القواعد الصوتية والإملائية",
                lessonTitle = "الدرس التمهيدي: المدود، السكون، الشدة، التنوين، واللام الشمسية والقمرية",
                lessonOrder = 1L,
                officialContent = """
                    مراجعة وتأسيس القواعد اللغوية والإملائية (من كتاب الوزارة وملحق الأضواء ٢٠٢٦/٢٠٢٧):
                    ١. حروف المد الثلاثة:
                       - المد بالألف (مثل: بَاب، كِتَاب، غُرَاب، صَبَاح، عَامِل) والحرف الممدود قبله مفتوح.
                       - المد بالواو (مثل: طُيُور، حُوت، صَبُور، مَحْمُود، نُور) والحرف الممدود قبله مضموم.
                       - المد بالياء (مثل: سَرِيع، صَدِيق، جَرِيدَة، قَدِيم، فِيل) والحرف الممدود قبله مكسور.
                    ٢. السكون والشدة:
                       - الحرف الساكن لا يُنطق وحده بل مع الحرف المتحرك قبله في مقطع صوتي واحد (مثل: قَلْـ/ـب، مَكْـ/ـتَب، أَحْـ/ـمَد).
                       - الحرف المشدد أصله حرفان: الأول ساكن والثاني متحرك (مثل: مُعَلِّم = مُـ / عَلْـ / لِـ / م، تَفَوَّقَ، الصَّبَاح).
                    ٣. التنوين: نون ساكنة تُنطق في آخر الاسم ولا تُكتب (تنوين بالفتح: كِتَابًا، تنوين بالضم: كِتَابٌ، تنوين بالكسر: كِتَابٍ).
                    ٤. اللام الشمسية واللام القمرية:
                       - اللام الشمسية: تُكتب ولا تُنطق، والحرف بعدها مشدد، وحروفها (ت، ث، د، ذ، ر، ز، س، ش، ص، ض، ط، ظ، ل، ن) مثل: (الشَّمْس، التِّلْمِيذ، السَّبُّورَة، النَّافِذَة، الصَّدِيق).
                       - اللام القمرية: تُكتب وتُنطق وعليها سكون، وحروفها (أ، ب، ج، ح، خ، ع، غ، ف، ق، ك، م، هـ، و، ي) مثل: (الْقَمَر، الْكِتَاب، الْفُرْشَاة، الْأَسْنَان، الْمَدْرَسَة).
                    ٥. التاء المربوطة (ـة / ة) والتاء المفتوحة (ت) وياء الملكية (كتابي، مدرستي، حقيبتي، أسناني).
                """.trimIndent(),
                keyTerms = "حروف المد (ا - و - ي)، الحرف الممدود، السكون، الشدة، التنوين، اللام الشمسية، اللام القمرية، ياء الملكية",
                sourceBookName = "كتاب اللغة العربية وملحق الأضواء - الصف الثاني الابتدائي ٢٠٢٦/٢٠٢٧",
                isCompleted = false
            ),
            "arabic_u1_l1" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.ARABIC.id,
                subjectNameAr = SubjectType.ARABIC.titleAr,
                unitTitle = "الوحدة الأولى: المعاملة الطيبة",
                lessonTitle = "الدرس الأول (استماع): يَوْمٌ جَمِيلٌ",
                lessonOrder = 2L,
                officialContent = """
                    نص الاستماع الرسمي (يوم جميل) - صفحة ١١٨ بكتاب الوزارة:
                    "أنا مريم، اليوم كان يوماً جميلاً، سأحكي لكم:
                    استيقظتُ مبكراً، ابتسمتُ لأمي وأبي، وقلتُ: (صباح الخير).
                    تناولتُ فطوري، ثم ذهبتُ إلى المدرسة.
                    لاحظتُ أن صديقتي (ليلى) حزينة، سألتها: ماذا بكِ يا ليلى؟
                    قالت ليلى: لقد نسيتُ قلمي.
                    قلتُ بابتسامة: لا تحزني.. معي قلمٌ.
                    قالت ليلى: شكراً لكِ، أنتِ صديقة جميلة.
                    في الفسحة، كنتُ ألعب مع أصدقائي، فصدمتُ زميلتي (فاطمة) دون قصد، فقلتُ لها: (أنا آسفة يا فاطمة).
                    قالت فاطمة مبتسمة: لا مشكلة، أنا أعرف أنكِ لم تقصدي.
                    تعرفون لماذا كان اليوم يوماً جميلاً؟ لأنني تعاملتُ مع الجميع معاملةً طيبةً، وهم أيضاً فعلوا ذلك."
                    القيم المستفادة: الاعتذار عند الخطأ، التسامح، التعاون مع الأصدقاء، الكلمة الطيبة.
                """.trimIndent(),
                keyTerms = "مبكراً (في أول النهار)، حزينة × سعيدة، دون قصد (دون تعمّد)، آسفة (أعتذر)، التسامح، الكلمة الطيبة",
                sourceBookName = "كتاب اللغة العربية الرسمي - الصف الثاني الابتدائي ٢٠٢٦/٢٠٢٧ (ص ٩ - ١٠)",
                isCompleted = false
            ),
            "arabic_u1_l2" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.ARABIC.id,
                subjectNameAr = SubjectType.ARABIC.titleAr,
                unitTitle = "الوحدة الأولى: المعاملة الطيبة",
                lessonTitle = "الدرس الثاني (قراءة): كَلِمَاتِي الْجَمِيلَةُ + المفرد والمثنى",
                lessonOrder = 3L,
                officialContent = """
                    نص القراءة الرسمي (كلماتي الجميلة) - صفحة ١٢ بكتاب الوزارة:
                    "حضرت ليلى اليومَ وهي تحملُ حقيبتها.
                    قالت سلمى: حقيبتكِ جميلةٌ يا ليلى.
                    ابتسمت ليلى، وقالت: شكراً يا سلمى.
                    في أثناء الحصة سقط قلمُ يوسف، فقالت له ليلى: هل تريد أن أحضرَه لك؟
                    قال يوسف: نعم؛ فأخذت ليلى القلمَ، وأعطته له، قال يوسف: شكراً لكِ.
                    ابتسمت المعلمةُ وقالت للتلاميذ: ما أجملَ كلماتكم! ... أشكرُ أمهاتكم وآباءكم؛ لأنهم علموكم كلماتٍ جميلةً، مثل: (شكراً، آسف، ملابسك جميلة، من فضلك)، هذه الكلماتُ تُسعد القلوبَ، وتُزيّن الكلامَ."
                    المفردات الجديدة:
                    - حقيبة (الجمع: حقائب)، التلاميذ (المفرد: التلميذ).
                    - جميلة (المضاد: قبيحة)، تُسعد (معناها: تُفرح، والمضاد: تُحزن)، تُزيّن (معناها: تُجمّل).
                    الأساليب والتراكيب (المفرد والمثنى):
                    - المثنى هو ما دل على اثنين أو اثنتين بزيادة (ان) أو (ين) على المفرد:
                      قلم + قلم = قلمان / قلمين، كتاب = كتابان / كتابين، ولد = ولدان / ولدين، وردة = وردتان / وردتين (التاء المربوطة تتحول لتاء مفتوحة عند التثنية).
                """.trimIndent(),
                keyTerms = "تُسعد (تُفرح)، تُزيّن (تُجمّل)، جمع حقيبة (حقائب)، المثنى (بزيادة ان أو ين)، شكرًا، من فضلك، آسف",
                sourceBookName = "كتاب اللغة العربية الرسمي - الصف الثاني الابتدائي ٢٠٢٦/٢٠٢٧ (ص ١١ - ٢٣)",
                isCompleted = false
            ),
            "arabic_u1_l3" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.ARABIC.id,
                subjectNameAr = SubjectType.ARABIC.titleAr,
                unitTitle = "الوحدة الأولى: المعاملة الطيبة",
                lessonTitle = "الدرس الثالث (قراءة): أُحِبُّ أَصْدِقَائِي + أسماء الإشارة",
                lessonOrder = 4L,
                officialContent = """
                    نص القراءة الرسمي (أحب أصدقائي) - صفحة ٢٥ بكتاب الوزارة:
                    "صباح الخير.. أنا أحمد، سأحكي لكم ما حدث في المدرسة اليوم.. دخلنا الفصل، فوجدنا زميلاً جديداً يجلس وحده في ركن الفصل.
                    قالت المعلمة: هذا (شريف) زميلكم الجديد، رحّبوا به.
                    قلنا بصوت واحد: أهلاً بك يا شريف.
                    اقتربتُ من شريف، وتحدثتُ معه، وعرفتُ أنه بارعٌ في الرسم مثلي، رسمنا معاً شجرةً جميلةً، أوراقها كثيرةٌ.
                    وفي وقت الفسحة جمعتُ أصدقائي، وذهبنا إليه، وتحدثنا معاً. حكى شريف عن مدرسته القديمة، وعن أصدقائه.
                    ضحكنا كثيراً، وعُدنا إلى الفصل. في نهاية اليوم قلتُ لشريف: أنا سعيدٌ بوجودك. ابتسم شريف وقال: وأنا سعيدٌ أيضاً."
                    المفردات الجديدة:
                    - بارع (معناها: ماهر)، عُدنا (معناها: رجعنا)، كثيرة (المضاد: قليلة).
                    - زميل (الجمع: زملاء)، رُكن (الجمع: أركان)، أصدقاء (المفرد: صديق).
                    الأساليب والتراكيب (أسماء الإشارة للقريب):
                    - هذا: للمفرد المذكر (هذا طبيبٌ، هذا تلميذٌ).
                    - هذه: للمفردة المؤنثة وللجمع غير العاقل (هذه طبيبةٌ، هذه أشجارٌ، هذه أقلامٌ ملونةٌ).
                    - هذان: للمثنى المذكر (هذان طبيبان، هذان تلميذان).
                    - هاتان: للمثنى المؤنث (هاتان طبيبتان، هاتان تلميذتان).
                    - هؤلاء: لجمع العاقل المذكر والمؤنث (هؤلاء أطباء، هؤلاء طبيبات، هؤلاء تلاميذ).
                """.trimIndent(),
                keyTerms = "بارع (ماهر)، عُدنا (رجعنا)، جمع رُكن (أركان)، أسماء الإشارة (هذا - هذه - هذان - هاتان - هؤلاء)",
                sourceBookName = "كتاب اللغة العربية الرسمي - الصف الثاني الابتدائي ٢٠٢٦/٢٠٢٧ (ص ٢٤ - ٣٦)",
                isCompleted = false
            ),
            "arabic_u1_l4" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.ARABIC.id,
                subjectNameAr = SubjectType.ARABIC.titleAr,
                unitTitle = "الوحدة الأولى: المعاملة الطيبة",
                lessonTitle = "الدرس الرابع (نشيد): الطِّفْلُ الْمُهَذَّبُ + قصة القطة والكلب",
                lessonOrder = 5L,
                officialContent = """
                    نشيد (الطفل المهذب) - صفحة ٣٨ بكتاب الوزارة:
                    أَنَا الطِّفْلُ الْمُؤَدَّبْ ... مَعَ النَّاسِ مُهَذَّبْ
                    أَقُولُ الصِّدْقَ دَوْمًا ... وَمَا كَذَبْتُ يَوْمًا
                    أُسَاعِدُ الْكِبَارَ ... وَأَنْصَحُ الصِّغَارَ
                    بِأَخْلَاقِ الْكِرَامِ ... سَنَحْيَا فِي سَلَامِ
                    معاني المفردات والشرح:
                    - مهذب: مؤدب، دوماً: دائماً، سنحيا: سنعيش.
                    - الكبار (المفرد: الكبير)، الصغار (المفرد: الصغير)، أخلاق (المفرد: خُلُق)، الكرام (المفرد: كريم).
                    - الكلمات التي لها نفس القافية والصوت: (المؤدب - مهذب)، (دوماً - يوماً)، (الكبار - الصغار).
                    قصة (القطة والكلب): بالتعاون نجعل كل عمل أسهل، فمساعدة الأصدقاء تخفف التعب.
                """.trimIndent(),
                keyTerms = "مهذب (مؤدب)، دوماً (دائماً)، سنحيا (سنعيش)، مفرد أخلاق (خُلُق)، مضاد الصدق (الكذب)",
                sourceBookName = "كتاب اللغة العربية الرسمي - الصف الثاني الابتدائي ٢٠٢٦/٢٠٢٧ (ص ٣٧ - ٤٣)",
                isCompleted = false
            ),
            "arabic_u2_l1_l2" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.ARABIC.id,
                subjectNameAr = SubjectType.ARABIC.titleAr,
                unitTitle = "الوحدة الثانية: عادات صحية",
                lessonTitle = "أُرَتِّبُ حَيَاتِي + قراءة: أَسْنَانِي قَوِيَّةٌ + ذلك وتلك",
                lessonOrder = 6L,
                officialContent = """
                    ١. نص الاستماع (أرتب حياتي):
                    استيقظت (لين) مبكراً، ورتبت ألعابها ووضعت كتبها على الرفوف، فصارت غرفتها جميلة ومنظمة. وفي المدرسة رتبت أغراضها بسرعة وسهولة؛ لأن النظام يساعدنا على إنهاء الأعمال بسرعة.
                    ٢. نص القراءة (أسناني قوية) - صفحة ٤٨ بكتاب الوزارة:
                    "جلس أشرف على سريره، ثم تذكر أنه لم يغسل أسنانه بالفرشاة، لكنه تكاسل، ثم نام؛ فرأى في الحلم حشرةً كبيرةً تهاجمه.. فجأة ظهرت فرشاة أسنان ضخمة، وقالت له: هذه سوسة الأسنان، لا تخف سوف أحميك منها. عندما رأت الحشرةُ الفرشاةَ الضخمة خافت وهربت بسرعة.
                    استيقظ أشرف، وأسرع إلى الحمام، وأمسك الفرشاة وغسل أسنانه جيداً.. وقالت الأم: فرشاتك تحبك وتحميك مثل الحارس الشخصي، فلا تتركها يوماً."
                    المفردات:
                    - تذكّر (المضاد: نَسِي)، ضخمة (معناها: كبيرة، والمضاد: صغيرة)، قَصَّ (معناها: حكى).
                    - أسنان (المفرد: سِنّ)، الحلم (الجمع: الأحلام)، المرآة (الجمع: المرايا).
                    الأساليب والتراكيب (أسماء الإشارة للبعيد):
                    - ذَلِكَ: للمفرد المذكر البعيد (ذلك قمرٌ منيرٌ، ذلك بيتٌ، ذلك صاروخٌ).
                    - تِلْكَ: للمفردة المؤنثة البعيدة (تلك شمسٌ مشرقةٌ، تلك سفينةٌ، تلك طائرةٌ).
                    التعبير الكتابي (البطاقة التعريفية): تتكون من (الاسم، العمر، مكان السكن/المدينة، الهوايات المفضلة).
                """.trimIndent(),
                keyTerms = "ضخمة (كبيرة)، قَصَّ (حكى)، جمع المرآة (المرايا)، أسماء الإشارة للبعيد (ذلك - تلك)، البطاقة التعريفية",
                sourceBookName = "كتاب اللغة العربية الرسمي - الصف الثاني الابتدائي ٢٠٢٦/٢٠٢٧ (ص ٤٥ - ٥٩)",
                isCompleted = false
            ),
            "arabic_u2_l3_l4" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.ARABIC.id,
                subjectNameAr = SubjectType.ARABIC.titleAr,
                unitTitle = "الوحدة الثانية: عادات صحية",
                lessonTitle = "طَعَامِي الصِّحِّيُّ + ظرف المكان والزمان + نشيد الْبَطَلُ الصَّغِيرُ",
                lessonOrder = 7L,
                officialContent = """
                    ١. نص القراءة (طعامي الصحي) - صفحة ٦١ بكتاب الوزارة:
                    كانت (آمنة) تشعر بالتعب وهي في طريق العودة من المدرسة ولا تقدر على حمل حقيبتها. قالت الأم: "لقد لاحظتُ أنكِ لا تأكلين بعض الأطعمة الضرورية للجسم؛ فيجب عليكِ تغيير عاداتكِ الغذائية".
                    أعدّت الأم لآمنة طبقاً غذائياً صحياً يحتوي على خضراوات وأرز ودجاج وفواكه. قالت الأم: "الغذاء الصحي يجعل جسمكِ قوياً وعقلكِ نشيطاً". ومنذ ذلك اليوم نفذت آمنة نصيحة أمها، فتحسنت صحتها وأصبحت تمشي بنشاط.
                    المفردات: أعدّت (حضّرت)، الضرورية (اللازمة)، بصعوبة (المضاد: بسهولة)، نشاط (المضاد: كسل)، طريق (الجمع: طُرُق)، نصيحة (الجمع: نصائح).
                    ٢. الأساليب والتراكيب (ظرف المكان وظرف الزمان):
                    - ظرف المكان: اسم يدل على مكان حدوث الفعل مثل: (أَمَامَ، خَلْفَ، فَوْقَ، تَحْتَ، دَاخِلَ، خَارِجَ، بَيْنَ).
                    - ظرف الزمان: اسم يدل على زمن حدوث الفعل مثل: (صَبَاحًا، مَسَاءً، لَيْلًا، ظُهْرًا، عَصْرًا، صَيْفًا، شِتَاءً، قَبْلَ، بَعْدَ).
                    ٣. نشيد (البطل الصغير) - صفحة ٧٤:
                    الْبَطَلُ الصَّغِيرُ .. عَادَاتُهُ مُفِيدَةْ / يَقُومُ فِي الْبُكُورِ .. لِلنُّورِ وَالسُّرُورِ / يُنَظِّفُ الْأَسْنَانَ .. يُرَتِّبُ الْمَكَانَ / طَعَامُهُ صِحِّيٌّ .. وَجِسْمُهُ قَوِيٌّ.
                    - البكور: أول النهار، السرور: السعادة والفرح (المضاد: الحزن)، مفيدة (المضاد: مضرة).
                """.trimIndent(),
                keyTerms = "أعدّت (حضّرت)، الضرورية (اللازمة)، البكور (أول النهار)، ظرف المكان (فوق/تحت/أمام/خلف/داخل)، ظرف الزمان (صباحاً/مساءً/ليلاً)",
                sourceBookName = "كتاب اللغة العربية الرسمي - الصف الثاني الابتدائي ٢٠٢٦/٢٠٢٧ (ص ٦٠ - ٨٠)",
                isCompleted = false
            ),
            "arabic_u3_all" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.ARABIC.id,
                subjectNameAr = SubjectType.ARABIC.titleAr,
                unitTitle = "الوحدة الثالثة: الرياضة والتكنولوجيا",
                lessonTitle = "حُسَام وَالْكُمْبِيُوتَر + فِي النَّادِي + نشيد رِيَاضَتِي + الأمر والنهي والأسماء الموصولة",
                lessonOrder = 8L,
                officialContent = """
                    ١. درس (حسام والكمبيوتر) - صفحة ٨٥:
                    يجلس حسام فترات طويلة أمام الكمبيوتر، فشعر بألم في عينيه (متألماً = متوجعاً). نصحته أمه بتحديد وقت قصير للألعاب الإلكترونية، وتقسيم (يوزّع = يقسّم) وقته بين الرياضة واللعب مع الأصحاب والمذاكرة والصلاة؛ لأن الجلوس طويلاً يؤذي (يضر × ينفع) العينين وقلة الحركة تضر بالصحة.
                    ٢. الأساليب: أسلوب الأمر وأسلوب النهي (صفحة ٩٠ - ٩١):
                    - أسلوب الأمر: طلب القيام بشيء (للمذكر: ارْسُمْ، رَتِّبْ، اغْسِلْ، حَافِظْ / للمؤنثة بإضافة ياء: ارْسُمِي، رَتِّبِي، اغْسِلِي، حَافِظِي).
                    - أسلوب النهي: طلب الامتناع عن شيء ويتكون من (لا الناهية + فعل مضارع) مثل: (لا تقتربْ من النار، لا ترفعْ صوتك، لا تكثرِي من أكل الحلوى).
                    ٣. درس (في النادي) والأسماء الموصولة (صفحة ٩٨ - ١٠٤):
                    ذهب نبيل وأخته سما للملعب يوم العطلة (الإجازة) مع مهند وفريدة. أسباب التفوق الرياضي: التعاون في كرة القدم، السرعة وقوة التركيز في كرة اليد، الاهتمام بالصحة والتدريب اليومي في الكاراتيه، وتنفيذ نصائح المدرب.
                    - الأسماء الموصولة: (الَّذِي) للمفرد المذكر (هنأتُ الولدَ الذي فاز)، و(الَّتِي) للمفردة المؤنثة (هنأتُ البنتَ التي فازت / الرياضةُ التي نمارسها مفيدة).
                    ٤. نشيد (رياضتي) - صفحة ١١١:
                    رِيَاضَتِي الْجَدِيدَةْ .. نَافِعَةٌ مُفِيدَةْ / جِسْمِي بِهَا قَوِيٌّ .. عَقْلِي بِهَا ذَكِيٌّ / تُرَتِّبُ الْأَوْقَاتَ .. تُجَمِّلُ الْحَيَاةَ / تُعَلِّمُ النِّظَامَ .. وَتَصْنَعُ السَّلَامَ.
                    - نافعة: مفيدة (المضاد: ضارة)، ترتب: تنظم، جمع عقل: عقول، جمع ذكي: أذكياء.
                """.trimIndent(),
                keyTerms = "متألماً (متوجعاً)، يوزّع (يقسّم)، العطلة (الإجازة)، أسلوب الأمر والنهي، الأسماء الموصولة (الذي - التي)، نشيد رياضتي",
                sourceBookName = "كتاب اللغة العربية الرسمي - الصف الثاني الابتدائي ٢٠٢٦/٢٠٢٧ (ص ٨١ - ١٢٠)",
                isCompleted = false
            ),

            // =========================================================================
            // ٢. التربية الدينية الإسلامية (الكتاب المدرسي الرسمي ٢٠٢٦/٢٠٢٧)
            // =========================================================================
            "religion_u1_all" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.RELIGION.id,
                subjectNameAr = SubjectType.RELIGION.titleAr,
                unitTitle = "الوحدة الأولى: التوكل على الله (تعالى)",
                lessonTitle = "الله (السميع) + سورة القدر والبينة + الأذان + نزول الوحي ونبي الله شعيب",
                lessonOrder = 1L,
                officialContent = """
                    محتوى الوحدة الأولى من كتاب التربية الدينية الإسلامية الرسمي (٢٠٢٦/٢٠٢٧ - ص ٧ إلى ٣٤):
                    ١. العقيدة - الله تعالى (السميع):
                    - الله سبحانه له الأسماء الحسنى، ومنها (السميع): أي الذي يسمع كل شيء في أي وقت وفي أي مكان، يسمع دعاءنا حتى لو كان صوتنا منخفضاً أو في سرنا. قال تعالى: ﴿لَيْسَ كَمِثْلِهِ شَيْءٌ وَهُوَ السَّمِيعُ الْبَصِيرُ﴾.
                    - قال رسول الله ﷺ للصحابة حين رفعوا أصواتهم: «أَيُّهَا النَّاسُ ارْبَعُوا عَلَى أَنْفُسِكُمْ (أي ارفقوا)، فَإِنَّكُمْ لَا تَدْعُونَ أَصَمَّ وَلَا غَائِبًا، إِنَّمَا تَدْعُونَ سَمِيعًا بَصِيرًا».
                    ٢. القرآن والتفسير - سورة القدر (مكية - ٥ آيات):
                    ﴿إِنَّا أَنْزَلْنَاهُ فِي لَيْلَةِ الْقَدْرِ (1) وَمَا أَدْرَاكَ مَا لَيْلَةُ الْقَدْرِ (2) لَيْلَةُ الْقَدْرِ خَيْرٌ مِنْ أَلْفِ شَهْرٍ (3) تَنَزَّلُ الْمَلَائِكَةُ وَالرُّوحُ فِيهَا بِإِذْنِ رَبِّهِمْ مِنْ كُلِّ أَمْرٍ (4) سَلَامٌ هِيَ حَتَّى مَطْلَعِ الْفَجْرِ (5)﴾.
                    - المعاني: (أنزلناه): أنزلنا القرآن الكريم في شهر رمضان، (القدر): الشرف والعظمة، (خير): أفضل، (الروح): سيدنا جبريل عليه السلام، (سلام): طمأنينة وأمن حتى طلوع الفجر.
                    ٣. العبادات - الأذان:
                    - الأذان هو نداءٌ إلى الصلاة وتذكير بدخول وقتها جماعةً، ويُنادى به خمس مرات في اليوم.
                    - ألفاظ الأذان: (الله أكبر ٤ مرات، أشهد أن لا إله إلا الله مرتين، أشهد أن محمداً رسول الله مرتين، حي على الصلاة مرتين، حي على الفلاح مرتين، الله أكبر مرتين، لا إله إلا الله مرة واحدة).
                    ٤. السير والشخصيات - نزول الوحي ونبي الله شعيب عليه السلام:
                    - كان النبي ﷺ يتأمل ويعبد الله في (غار حراء)، فنزل عليه الملك (جبريل عليه السلام) بأول آيات من القرآن من سورة العلق: ﴿اقْرَأْ بِاسْمِ رَبِّكَ الَّذِي خَلَقَ﴾. وعاد النبي ﷺ إلى بيته خائفاً يقول: «دَثِّرُونِي، دَثِّرُونِي» (غطوني)، فطمأنته زوجته السيدة خديجة رضي الله عنها وذهبت به إلى ابن عمها ورقة بن نوفل.
                    - نبي الله شعيب عليه السلام: أرسله الله إلى قوم (مدين) الذين كانوا يعملون بالتجارة وينقصون الكيل والميزان ويغشون الناس ويعبدون الأصنام، فدعاهم لعبادة الله والأمانة في البيع والشراء.
                    ٥. القيم والأخلاق - التوكل على الله: هو بذل الجهد وإتقان العمل والمذاكرة ثم الاعتماد على الله في النتيجة، أما ترك العمل والكسل فهو (تواكل) منهي عنه.
                """.trimIndent(),
                keyTerms = "السميع، اربعوا (ارفقوا)، ليلة القدر، الروح (جبريل عليه السلام)، الأذان، غار حراء، دثّروني (غطوني)، شعيب وقوم مدين، التوكل × التواكل",
                sourceBookName = "كتاب التربية الدينية الإسلامية الرسمي - الصف الثاني الابتدائي ٢٠٢٦/٢٠٢٧ (ص ٧ - ٣٤)",
                isCompleted = false
            ),
            "religion_u2_all" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.RELIGION.id,
                subjectNameAr = SubjectType.RELIGION.titleAr,
                unitTitle = "الوحدة الثانية: من نعم الله (تعالى)",
                lessonTitle = "الله (البصير) + سورة التين والعلق + الإقامة + الدعوة السرية ونبي الله موسى",
                lessonOrder = 2L,
                officialContent = """
                    محتوى الوحدة الثانية من كتاب التربية الدينية الإسلامية الرسمي (٢٠٢٦/٢٠٢٧ - ص ٣٥ إلى ٦١):
                    ١. العقيدة - الله تعالى (البصير):
                    - البصير هو الذي يرى كل شيء في الأرض وفي السماء صغيراً كان أو كبيراً، ويرى أفعالنا ويعلم ما في قلوبنا. قال النبي ﷺ: «الْإِحْسَانُ أَنْ تَعْبُدَ اللَّهَ كَأَنَّكَ تَرَاهُ، فَإِنْ لَمْ تَكُنْ تَرَاهُ فَإِنَّهُ يَرَاكَ».
                    ٢. القرآن والتفسير - سورة التين (مكية - ٨ آيات):
                    ﴿وَالتِّينِ وَالزَّيْتُونِ (1) وَطُورِ سِينِينَ (2) وَهَذَا الْبَلَدِ الْأَمِينِ (3) لَقَدْ خَلَقْنَا الْإِنْسَانَ فِي أَحْسَنِ تَقْوِيمٍ (4) ثُمَّ رَدَدْنَاهُ أَسْفَلَ سَافِلِينَ (5) إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ فَلَهُمْ أَجْرٌ غَيْرُ مَمْنُونٍ (6) فَمَا يُكَذِّبُكَ بَعْدُ بِالدِّينِ (7) أَلَيْسَ اللَّهُ بِأَحْكَمِ الْحَاكِمِينَ (8)﴾.
                    - المعاني: (طور سينين): جبل الطور بسيناء في مصر الذي كلّم الله عليه موسى، (البلد الأمين): مكة المكرمة، (أحسن تقويم): أفضل صورة، (غير ممنون): غير مقطوع.
                    ٣. العبادات - الإقامة:
                    - إقامة الصلاة هي إعلامٌ بقرب بدء الصلاة، وتُقال بعد الأذان وقبل الدخول في الصلاة مباشرة، ويُزاد فيها: (قَدْ قَامَتِ الصَّلَاةُ، قَدْ قَامَتِ الصَّلَاةُ).
                    ٤. السير والشخصيات - بداية الدعوة السرية ونبي الله موسى عليه السلام:
                    - استمرت الدعوة السرية ٣ سنوات، وأول من آمن بالنبي ﷺ: من النساء (السيدة خديجة رضي الله عنها)، ومن الرجال (أبو بكر الصديق رضي الله عنه)، ومن الصبيان (علي بن أبي طالب رضي الله عنه وكان عمره ١٠ سنين)، ومن الموالي (زيد بن حارثة رضي الله عنه).
                    - قصة نبي الله موسى عليه السلام: أوحى الله إلى أم موسى أن تضعه في صندوق (تابوت) وتلقيه في نهر النيل لحمايته من فرعون، فالتقطه آل فرعون وأحبته زوجة فرعون، وأرضعته أمه بفضل الله، ثم كلّمه الله عند جبل الطور بسيناء وأرسله لفرعون، ونجّى الله موسى وقومه بشق البحر وأغرق فرعون.
                    ٥. القيم - الأمانة في الحفاظ على الجسم والعقل: بالغذاء الصحي والرياضة والنظافة وطلب العلم النافع.
                """.trimIndent(),
                keyTerms = "البصير، الإحسان، طور سينين (جبل الطور بسيناء)، البلد الأمين (مكة)، أحسن تقويم، غير ممنون (غير مقطوع)، قد قامت الصلاة، السابقون الأولون، موسى عليه السلام",
                sourceBookName = "كتاب التربية الدينية الإسلامية الرسمي - الصف الثاني الابتدائي ٢٠٢٦/٢٠٢٧ (ص ٣٥ - ٦١)",
                isCompleted = false
            ),
            "religion_u3_all" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.RELIGION.id,
                subjectNameAr = SubjectType.RELIGION.titleAr,
                unitTitle = "الوحدة الثالثة: الشكر لله (تعالى)",
                lessonTitle = "الله (الشكور) + سورة الشرح والضحى + صلاة الجماعة + الدعوة الجهرية ونبي الله داود",
                lessonOrder = 3L,
                officialContent = """
                    محتوى الوحدة الثالثة من كتاب التربية الدينية الإسلامية الرسمي (٢٠٢٦/٢٠٢٧ - ص ٦٢ إلى ٨٧):
                    ١. العقيدة - الله تعالى (الشكور):
                    - الشكور يعني أن الله يتقبل من عباده المؤمنين كل عمل صالح مهما كان بسيطاً ويكافئهم عليه ثواباً عظيماً مضاعفاً. ونشكر الله بقلوبنا وألسنتنا (الحمد لله) وبأفعالنا الصالحة.
                    ٢. القرآن والتفسير - سورة الشرح (مكية - ٨ آيات):
                    ﴿أَلَمْ نَشْرَحْ لَكَ صَدْرَكَ (1) وَوَضَعْنَا عَنْكَ وِزْرَكَ (2) الَّذِي أَنْقَضَ ظَهْرَكَ (3) وَرَفَعْنَا لَكَ ذِكْرَكَ (4) فَإِنَّ مَعَ الْعُسْرِ يُسْرًا (5) إِنَّ مَعَ الْعُسْرِ يُسْرًا (6) فَإِذَا فَرَغْتَ فَانْصَبْ (7) وَإِلَى رَبِّكَ فَارْغَبْ (8)﴾.
                    - المعاني: (وزرك): أحمالك الثقيلة، (أنقض): أتعب وأرهق، (العسر): الأمر الصعب، (يسراً): الأمر السهل، (فرغت): انتهيت من عملك، (فانصب): اجتهد في العبادة.
                    ٣. العبادات - صلاة الجماعة:
                    - تصلى من اثنين أو أكثر في صفوف متساوية خلف الإمام ونتابعه في كل أفعاله. قال النبي ﷺ: «صَلَاةُ الْجَمَاعَةِ تَفْضُلُ صَلَاةَ الْفَذِّ (الفرد) بِسَبْعٍ وَعِشْرِينَ دَرَجَةً».
                    ٤. السير والشخصيات - الدعوة الجهرية ونبي الله داود عليه السلام:
                    - صعد النبي ﷺ على جبل (الصفا) بمكة وجهر بالدعوة للإسلام، فعارضه عمه أبو لهب فأنزل الله سورة المسد: ﴿تَبَّتْ يَدَا أَبِي لَهَبٍ وَتَبَّ﴾.
                    - نبي الله داود عليه السلام: كان يرعى الأغنام ويسبح الله بصوته العذب الجميل فتردد الجبال والطيور معه التسبيح، وهزم العملاق (جالوت) بحجر صغير من مقلاعه، وآتاه الله الملك والنبوة وكتاب (الزبور)، وكان يعمل حداداً يصنع الدروع بيده ويصوم يوماً ويفطر يوماً.
                """.trimIndent(),
                keyTerms = "الشكور، سورة الشرح (وزرك - أنقض - العسر - فانصب)، صلاة الجماعة (٢٧ درجة)، جبل الصفا وسورة المسد، نبي الله داود والزبور وجالوت",
                sourceBookName = "كتاب التربية الدينية الإسلامية الرسمي - الصف الثاني الابتدائي ٢٠٢٦/٢٠٢٧ (ص ٦٢ - ٨٧)",
                isCompleted = false
            ),

            // =========================================================================
            // ٣. Math - الرياضيات لغات (Primary 2 Official Book 2026/2027 + Revision)
            // =========================================================================
            "math_ch1_graphs" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.MATH.id,
                subjectNameAr = SubjectType.MATH.titleAr,
                unitTitle = "Chapter 1: Graphs, Charts, Bar Graphs & Pictographs",
                lessonTitle = "Lessons 1-1 to 1-3: Reading, Comparing & Interpreting Graphs and Pictographs",
                lessonOrder = 1L,
                officialContent = """
                    Primary 2 Mathematics Official Book (2026/2027) - Chapter 1: Graphs and Charts:
                    1) Organizing Numbers in a Graph and a Chart:
                       - When organizing items, mark each item and draw circles (O) or shade boxes starting from the bottom.
                       - When you show the counted numbers in a GRAPH (الرسم البياني), you can instantly see which item has more or less.
                       - When you show the counted numbers in a CHART / TABLE (الجدول), the exact number of each item becomes instantly clear.
                    2) Bar Graphs with Different Scales (التمثيل البياني بالأعمدة بمقياس ١ أو ٢ أو ١٠):
                       - Always check the scale on the vertical axis (0, 1, 2... or 0, 2, 4, 6... or 0, 10, 20, 30...).
                       - "How many in all / altogether?" -> Use Addition (+). Example: 6 dogs + 3 cats = 9 animals.
                       - "How many more ... than ...?" -> Use Subtraction (-) to find the difference. Example: 6 green - 4 yellow = 2 students.
                    3) Pictographs (التمثيل البياني بالصور):
                       - Always read the KEY (المفتاح) first!
                       - In the "Pick a Flower" pictograph: 1 whole flower symbol = 2 flowers, and a half flower symbol = 1 flower.
                """.trimIndent(),
                keyTerms = "Graph (رسم بياني), Chart/Table (جدول), Bar Graph (أعمدة بيانية), Pictograph (تمثيل بالصور), Scale (مقياس التدرج), How many more (كم يزيد = طرح), In all / Total (الإجمالي = جمع)",
                sourceBookName = "Primary 2 Mathematics Term 1 Official Book 2026/2027 (Pages 4–15)",
                isCompleted = false
            ),
            "math_ch2_ch3_vertical" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.MATH.id,
                subjectNameAr = SubjectType.MATH.titleAr,
                unitTitle = "Chapters 2 & 3: Vertical Addition and Subtraction",
                lessonTitle = "Vertical Method, Regrouping (Carry Over / Borrow) & Checking Answers",
                lessonOrder = 2L,
                officialContent = """
                    Primary 2 Mathematics Official Book (2026/2027) - Chapters 2 & 3:
                    1) Vertical Addition (الجمع الرأسي):
                       - A calculation where you line up the place values vertically is called the VERTICAL METHOD (الطريقة الرأسية).
                       - Always start calculating from the ONES PLACE (خانة الآحاد), then the TENS PLACE (خانة العشرات).
                       - Regrouping / Carry Over (إعادة التجميع / الحمل): If the sum of the Ones place is 10 or more, CARRY OVER 1 (10 ones = 1 ten) to the Tens place!
                         Example: 26 + 17 -> Ones: 6 + 7 = 13 (write 3 in Ones place, carry 1 to Tens place). Tens: 1 + 2 + 1 = 4. Answer = 43.
                       - Commutative Property of Addition (خاصية الإبدال): Even if you exchange the order of the two numbers (addends), the answer is the same: 13 + 18 = 18 + 13 = 31.
                    2) Vertical Subtraction (الطرح الرأسي):
                       - Line up Ones under Ones and Tens under Tens. Subtract starting from the Ones place.
                       - Regrouping / Borrowing in Subtraction (الاستلاف): In 32 - 15, since you cannot subtract 5 from 2 in the Ones place, TAKE 1 TEN (which is 10 ones) from the 3 Tens (leaving 2 Tens), so Ones become 12. Then 12 - 5 = 7, and Tens: 2 - 1 = 1. Answer = 17.
                    3) Relation Between Subtraction and Addition (العلاقة بين الطرح والجمع - Fact Family):
                       - In 54 - 26 = 28: 54 is the Minuend (المطروح منه), 26 is the Subtrahend (المطروح), and 28 is the Difference (الفرق / باقي الطرح).
                       - Check your subtraction using addition: Difference + Subtrahend = Minuend (28 + 26 = 54).
                """.trimIndent(),
                keyTerms = "Vertical Method (الطريقة الرأسية), Ones Place (خانة الآحاد), Tens Place (خانة العشرات), Carry Over / Regroup (إعادة التجميع), Minuend (المطروح منه), Subtrahend (المطروح), Difference (الفرق)",
                sourceBookName = "Primary 2 Mathematics Term 1 Official Book 2026/2027 (Pages 16–47)",
                isCompleted = false
            ),
            "math_ch4_ch5_time_length_shapes" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.MATH.id,
                subjectNameAr = SubjectType.MATH.titleAr,
                unitTitle = "Chapters 4 & 5: Time, Duration, Length (cm/mm) & Shapes",
                lessonTitle = "Telling Time (AM/PM), Measuring Length (cm & mm), and 2D/3D Shapes",
                lessonOrder = 3L,
                officialContent = """
                    Primary 2 Mathematics Official Book (2026/2027) - Chapters 4 & 5:
                    1) Time and Duration (الوقت والمدة الزمنية):
                       - The interval between one Time and another Time is called DURATION (المدة الزمنية).
                       - The long hand (عقرب الدقائق) moves 1 scale unit in 1 minute, and completes one full rotation in 1 hour = 60 minutes.
                       - 1 day = 24 hours. Time before 12 o'clock noon is called AM (صباحاً). Time after 12 o'clock noon is called PM (مساءً). 12:00 in the middle of the day is called Noon (ظهراً).
                       - Reading clocks: Half past = :30, Quarter past = :15, Quarter to = :45.
                    2) Units of Length (وحدات قياس الطول: السنتيمتر والمليمتر):
                       - Centimeter (cm) is used to measure length. When 1 cm is divided into 10 equal parts, 1 part is 1 millimeter (mm).
                       - Rule: 1 cm = 10 mm (Example: 3 cm 2 mm = 32 mm; 45 mm = 4 cm 5 mm).
                       - When adding or subtracting lengths, calculate units of the same type together (cm with cm, and mm with mm).
                    3) 2D and 3D Geometric Shapes (الأشكال الهندسية ثنائية وثلاثية الأبعاد):
                       - 2D Shapes: Triangle (3 sides, 3 vertices), Square (4 equal sides, 4 vertices), Rectangle (4 sides, 4 vertices), Trapezoid (4 sides), Rhombus (4 equal sides), Pentagon (5 sides, 5 vertices), Hexagon (6 sides, 6 vertices).
                       - 3D Shapes: Cube & Rectangular Prism (6 faces, 12 edges, 8 vertices), Square-based Pyramid (5 faces, 8 edges, 5 vertices), Cylinder (2 circular faces, 0 edges, 0 vertices), Sphere (0 faces, 0 edges, 0 vertices).
                """.trimIndent(),
                keyTerms = "Duration (المدة الزمنية), 1 hour = 60 minutes, AM (صباحاً) / PM (مساءً), Centimeter cm (سنتيمتر), Millimeter mm (١ سم = ١٠ مم), Sides (أضلاع), Vertices (رؤوس), Faces (أوجه), Edges (أحرف)",
                sourceBookName = "Primary 2 Mathematics Term 1 Official Book 2026/2027 (Pages 48–67)",
                isCompleted = false
            ),
            "math_ch6_ch7_ch8_ch9_numbers_capacity_mental" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.MATH.id,
                subjectNameAr = SubjectType.MATH.titleAr,
                unitTitle = "Chapters 6 to 9 & Revision: Numbers to 1,000, Capacity, Mass & Fractions",
                lessonTitle = "3-Digit Numbers, Capacity (L/dL/mL), Mass (kg/gm), Mental Math & Fractions",
                lessonOrder = 4L,
                officialContent = """
                    Primary 2 Mathematics Official Book (2026/2027) - Chapters 6, 7, 8, 9 & Revision Models:
                    1) Numbers Bigger Than 100 (Up to 1,000):
                       - 10 tens make 1 hundred (100). 10 hundreds make One Thousand (1,000).
                       - Example: 234 has 2 in the Hundreds place (200), 3 in the Tens place (30), and 4 in the Ones place (4).
                       - Expanded Form: 640 = 600 + 40. Also 640 is made by collecting 64 tens!
                       - Comparing 3-digit numbers: Compare the Hundreds place first (e.g., 359 > 298). If Hundreds are equal, compare the Tens place (359 < 362).
                    2) Units of Capacity (وحدات السعة: اللتر والديسيلتر والمليلتر) & Mass (الكتلة):
                       - 1 Liter (L) = 10 deciliters (dL) = 1,000 milliliters (mL).
                       - 1 deciliter (dL) = 100 milliliters (mL). Example: 1 L 4 dL = 14 dL; 3 dL 50 mL = 350 mL.
                       - Mass: Grams (gm) for light objects (ring, feather, pen) and Kilograms (kg) for heavy objects (goat, bicycle, child).
                    3) 3-Digit Vertical Addition & Subtraction with Regrouping (Chapter 8):
                       - In 74 + 68 = 142: carry 1 ten from Ones, then carry 1 hundred from Tens!
                       - In 107 - 59 = 48: when Tens place is 0, first take 1 hundred (= 10 tens) from the Hundreds place, then take 1 ten (= 10 ones) to the Ones place.
                    4) Mental Math Strategies (Chapter 9) & Fractions (الكسور - من ملحق التأسيس السليم):
                       - Using parentheses ( ) to make 10 or multiples of 10 first: 12 + (7 + 3) = 12 + 10 = 22.
                       - Splitting numbers to make a multiple of 10: 37 + 9 = 37 + (3 + 6) = 40 + 6 = 46.
                       - Fractions (الكسور): Numerator (البسط - أعلى الكسر) and Denominator (المقام - أسفل الكسر).
                         * One-half = 1/2 (نصفان متساويان 2 halves = 1 whole).
                         * One-third = 1/3, Two-thirds = 2/3 (3 thirds = 1 whole).
                         * One-fourth (quarter) = 1/4, Two-fourths = 2/4 = 1/2, Three-fourths = 3/4 (4 fourths = 1 whole).
                """.trimIndent(),
                keyTerms = "One Thousand 1,000 (ألف), Expanded Form (الصيغة الممتدة), Liter L / Deciliter dL / Milliliter mL (وحدات السعة), Grams & Kilograms (الجرام والكيلوجرام), Fraction (كسر), Numerator (البسط), Denominator (المقام), Halves/Thirds/Fourths",
                sourceBookName = "Primary 2 Mathematics Official Book 2026/2027 (Pages 68–125) & AlTaasis Revision",
                isCompleted = false
            ),

            // =========================================================================
            // ٤. English Connect (Official Primary 2 Student Book 2026/2027 - Units 1 to 6)
            // =========================================================================
            "connect_u1_u2" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.CONNECT.id,
                subjectNameAr = SubjectType.CONNECT.titleAr,
                unitTitle = "Units 1 & 2: Let's Get Started! & Colors, Shapes, and Numbers",
                lessonTitle = "Classroom Greetings, Objects, Colors, Shapes & Counting to 100",
                lessonOrder = 1L,
                officialContent = """
                    Official Primary 2 English Student Book (Term 1 - 2026/2027) - Units 1 & 2:
                    [Unit 1: Let's Get Started! - Pages 5 to 16]
                    1) Greetings & Self-Introduction:
                       - "Hello, I'm Ali. What's your name?" -> "My name is Ahmed."
                       - "Good morning, Ms. Amira." / "May I sit down?" -> "Yes, please." / "Goodbye!"
                    2) Classroom Objects Vocabulary:
                       - teacher (معلم/معلمة), student (تلميذ), chair (كرسي), desk (مكتب), board (سبورة), window (شباك), door (باب), pen (قلم جاف), pencil (قلم رصاص), pencil case (مقلمة), book (كتاب), notebook (كراسة), ruler (مسطرة), eraser (أستيكة/ممحاة), bag (حقيبة).
                    3) Language in Use:
                       - "What is this?" -> "This is a chair." / "This is an eraser." (for near objects)
                       - "That is a door." / "That is a window." (for far objects)
                       - "Where is the door?" -> "It's over there."
                       - "This is my pencil case." / "I have a bag."
                    4) Unit 1 Phonics (Short a, e, i sounds):
                       - Short /a/: cat (قطة), hat (قبعة), fan (مروحة), man (رجل), map (خريطة), cap (كاب).
                       - Short /e/ & /i/: pen (قلم), ten (عشرة), hen (دجاجة), zip (سوستة), fig (تين), bin (سلة مهملات).

                    [Unit 2: Colors, Shapes, and Numbers - Pages 17 to 30]
                    1) Colors & Shapes:
                       - Colors: red (أحمر), blue (أزرق), green (أخضر), orange (برتقالي), yellow (أصفر).
                       - Shapes: circle (دائرة), square (مربع), triangle (مثلث).
                       - Sentences: "What color is this? It is blue." / "This is a red circle." / "I see a blue triangle." / "The green square is on the desk."
                    2) Numbers (1–10 and Counting by 10s to 100) & Singular/Plural:
                       - Numbers: one (1), two (2), three (3), four (4), five (5), six (6), seven (7), eight (8), nine (9), ten (10), twenty (20), thirty (30), forty (40), fifty (50), sixty (60), seventy (70), eighty (80), ninety (90), one hundred (100).
                       - Singular vs. Plural (المفرد والجمع بإضافة s): "I have one book." vs "I have two chairs."
                       - "How many pencils are there?" -> "There are three pencils." / "There is one board."
                    3) Unit 2 Phonics (Short o, u & Long a):
                       - Short /o/ & /u/: dog (كلب), box (صندوق), fox (ثعلب), sun (شمس), cup (كوب), bun (كعكة).
                       - Long /a/ (magic e): cake (كعكة/تورتة), lake (بحيرة), gate (بوابة), cave (كهف), snake (ثعبان), grapes (عنب).
                """.trimIndent(),
                keyTerms = "What's your name? (ما اسمك؟), May I sit down? (هل لي أن أجلس؟), Pencil case (مقلمة), Eraser (ممحاة), Ruler (مسطرة), Circle/Square/Triangle (دائرة/مربع/مثلث), How many (كم العدد؟), There is / There are (يوجد للمفرد / للجمع)",
                sourceBookName = "Primary 2 English Official Student Book Term 1 (2026/2027 - Pages 5–30)",
                isCompleted = false
            ),
            "connect_u3_u4" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.CONNECT.id,
                subjectNameAr = SubjectType.CONNECT.titleAr,
                unitTitle = "Units 3 & 4: Classroom Actions & Everyday Life (My Family)",
                lessonTitle = "Classroom Commands, Family Members, Possessives & Daily Routines",
                lessonOrder = 2L,
                officialContent = """
                    Official Primary 2 English Student Book (Term 1 - 2026/2027) - Units 3 & 4:
                    [Unit 3: Classroom Actions & Routines - Pages 31 to 48]
                    1) Action Verbs in the Classroom:
                       - read (يقرأ), write (يكتب), draw (يرسم), color (يلون), clean (ينظف), play (يلعب), sit (يجلس), stand (يقف), sing (يغني), raise (يرفع), open (يفتح), close (يغلق).
                    2) Classroom Instructions & Imperatives (أفعال الأمر في الفصل):
                       - "Stand up, please." (قف من فضلك) / "Sit down, please." (اجلس من فضلك)
                       - "Open your book." (افتح كتابك) / "Close your book." (أغلق كتابك)
                       - "Raise your hand." (ارفع يدك) / "Write your name." (اكتب اسمك) / "Draw a circle." (ارسم دائرة) / "Clap!" (صفّق).
                    3) Reading & Writing Sentences:
                       - "What do you do in the classroom?" -> "I sit on my chair. I read my book. I write in my notebook. I draw a picture. I play with my friends. I clean my desk."

                    [Unit 4: Everyday Life - Pages 49 to 63]
                    1) Family Members & Possessive Adjectives (أفراد العائلة وصفات الملكية):
                       - father / dad (أب), mother / mom (أم), sister (أخت), brother (أخ), grandmother (جدة), grandfather (جد).
                       - We use "my" when something belongs to the speaker: "This is my father. He is tall." / "This is my grandmother. She is kind."
                       - We use "your" when something belongs to the listener: "This is your brother." / "This is your bag."
                       - Possessive ('s) للملكية: "Ahmed has a car. It's Ahmed's car."
                    2) Daily Routines & The Present Simple Tense (زمن المضارع البسيط للروتين اليومي):
                       - Verbs: wake up (يستيقظ), eat (يأكل), play (يلعب), sleep (ينام), brush (يفرش أسنانه/يمشط شعره), read (يقرأ), wash (يغسل), walk (يمشي).
                       - Grammar: With I/We/You/They use base verb: "I wake up at 7:00 am." / "I brush my teeth at night." / "They play after school."
                         With He/She/It add (-s / -es): "He eats breakfast." / "She sleeps early." / "He washes his face."
                    3) Unit 4 Phonics (Long o, Long u, and Digraph 'th'):
                       - Long /o/ & /u/: nose (أنف), rose (وردة), glue (صمغ), cube (مكعب).
                       - Digraph /th/: thin (نحيف/رفيع), thick (سميك), thumb (إصبع الإبهام), thunder (رعد), math (رياضيات), teeth (أسنان).
                """.trimIndent(),
                keyTerms = "Stand up / Sit down (قف / اجلس), Raise your hand (ارفع يدك), Open/Close your book, Grandfather/Grandmother (الجد/الجدة), My / Your (ملكي / ملكك), I wake up (أستيقظ), I brush my teeth (أغسل أسناني), Digraph th (thin/thick/thumb/teeth)",
                sourceBookName = "Primary 2 English Official Student Book Term 1 (2026/2027 - Pages 31–63)",
                isCompleted = false
            ),
            "connect_u5_u6" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.CONNECT.id,
                subjectNameAr = SubjectType.CONNECT.titleAr,
                unitTitle = "Units 5 & 6: My Home & Rooms at Home",
                lessonTitle = "Furniture, Prepositions of Place (in/on/under/next to), Rooms & Phonics sh/ch",
                lessonOrder = 3L,
                officialContent = """
                    Official Primary 2 English Student Book (Term 1 - 2026/2027) - Units 5 & 6:
                    [Unit 5: My Home - Pages 64 to 81]
                    1) Household Items & Furniture:
                       - table (منضدة/طاولة), chair (كرسي), bed (سرير), door (باب), window (شباك), lamp (مصباح/أباجورة), cup (فنجان/كوب), plate (طبق), couch (كنبة/أريكة), desk (مكتب).
                    2) Prepositions of Place (حروف الجر الخاصة بالمكان):
                       - on (على): "The lamp is on the table."
                       - under (تحت): "The ball is under the table."
                       - in (في / داخل): "The bed is in the bedroom." / "There are flowers in the garden."
                       - next to (بجوار / بجانب): "The cup is next to the plate." / "The cat is next to the house."
                    3) Actions at Home (Present Simple with Who questions):
                       - "My family cooks dinner." / "I read a book at night." / "I help my family wash the dishes." / "My sister watches TV."
                       - "Who washes the dishes?" -> "My brother washes the dishes."
                       - "Who helps your family cook?" -> "I help my mom cook."
                    4) Unit 5 Phonics (Digraphs 'sh' and voiced 'th'):
                       - Voiced /th/: mother (أم), father (أب), brother (أخ), this (هذا للقريب), that (ذلك للبعيد).
                       - Digraph /sh/: sheep (خروف), ship (سفينة), shop (محل/متجر), fish (سمكة), dish (طبق), brush (فرشاة).

                    [Unit 6: Rooms at Home - Pages 82 to 101]
                    1) Rooms in a House (غرف المنزل):
                       - kitchen (مطبخ - where we cook food), living room (غرفة المعيشة - where we watch TV and relax), bedroom (غرفة النوم - where we sleep), bathroom (الحمام - where we wash our face and brush teeth), dining room (غرفة الطعام - where we eat meals), garden (الحديقة), hall (الصالة/الممر), balcony (الشرفة/البلكونة).
                    2) Language in Use (There is / There are & Where questions):
                       - "There is a kitchen in my house." (مفرد) / "There are three bedrooms in my house." (جمع)
                       - "Where do you eat?" -> "I eat in the dining room."
                       - "Where do you watch TV?" -> "I watch TV in the living room."
                    3) Unit 6 Phonics (Digraph 'ch'):
                       - Digraph /ch/: cheese (جبن), chicken (دجاج), chin (ذقن), lunch (غداء), beach (شاطئ).
                """.trimIndent(),
                keyTerms = "Prepositions: in (في), on (على), under (تحت), next to (بجوار), Kitchen (مطبخ), Living room (غرفة معيشة), Bedroom (غرفة نوم), Bathroom (حمام), Dining room (غرفة طعام), Balcony (شرفة), Digraphs sh & ch",
                sourceBookName = "Primary 2 English Official Student Book Term 1 (2026/2027 - Pages 64–103)",
                isCompleted = false
            ),

            // =========================================================================
            // ٥. English Advanced (Mr Sobhy Borda Lesson Preparation & Advanced Practice)
            // =========================================================================
            "connect_plus_borda_u1_u3" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.CONNECT_PLUS.id,
                subjectNameAr = SubjectType.CONNECT_PLUS.titleAr,
                unitTitle = "Advanced English Notebook: Units 1, 2 & 3 (Grammar, Phonics & Dialogues)",
                lessonTitle = "Structured Dialogues, Sentence Reordering, Singular/Plural & Imperatives",
                lessonOrder = 1L,
                officialContent = """
                    English Language Lesson Preparation Notebook (Mr Sobhy Borda - Primary Two First Term - Units 1 to 3):
                    1) Unit 1 Advanced Dialogues & Sentence Building (Let's Get Started!):
                       - Role Play 1 (Arriving at School):
                         Student A: "Hello! Good morning. What's your name?"
                         Student B: "Good morning! My name is Zein. Nice to meet you. May I sit down?"
                         Student A: "Yes, please. This is a chair and that is a desk."
                       - Sentence Order Rule: [Subject + Verb + Object].
                         Examples: "This is my pencil case." / "I have a bag." / "He is a teacher." / "Where is the eraser? — It's over there."
                       - Phonics Mastery (Short Vowels a, e, i):
                         * Short /a/: cat, hat, map, man, cap, fan.
                         * Short /e/: pen, ten, hen, red, bed, desk.
                         * Short /i/: zip, bin, fig, big, pin, sit.
                    2) Unit 2 Advanced Structures (Colors, Shapes, and Numbers):
                       - Describing Objects with Adjective + Noun order: [Article + Color + Shape/Object].
                         Examples: "This is a red circle." / "I see a blue triangle." / "The yellow square is on the desk." / "The ball is blue and round."
                       - Counting & Plural Nouns Rule:
                         * Singular (1 item): "There is one desk." / "I have one ruler."
                         * Plural (2 or more items -> add -s): "There are ten pens." / "How many books are there? — There are आठ (eight) books."
                         * Counting by tens: ten (10), twenty (20), thirty (30), forty (40), fifty (50), sixty (60), seventy (70), eighty (80), ninety (90), one hundred (100).
                       - Phonics Mastery (CVC Short o/u vs. Long a):
                         * Short /o/: dog, box, fox. Short /u/: sun, cup, bun.
                         * Long /a/ (a_e): cake, lake, gate, cave, snake, grapes.
                    3) Unit 3 Advanced Structures (Classroom Actions & Routines):
                       - Imperative Sentences (Giving polite classroom commands):
                         "Stand up, please." / "Sit down, please." / "Open your book." / "Close your book." / "Raise your hand." / "Write your name on the board."
                       - Expressing Daily Class Routines with Prepositional Phrases:
                         "In class, I sit on my chair. I read my book. I write in my notebook. I play with my friends. I clean my desk."
                """.trimIndent(),
                keyTerms = "Sentence Order (Subject + Verb + Object), Adjective before Noun (a red circle), Singular vs Plural (-s), Imperative Commands (Stand up / Open your book), Short & Long Vowels",
                sourceBookName = "Mr Sobhy Borda - English Language Lesson Preparation Notebook Grade 2 (Pages 1–34)",
                isCompleted = false
            ),
            "connect_plus_borda_u4_u6" to CurriculumLesson(
                userId = userId,
                subjectId = SubjectType.CONNECT_PLUS.id,
                subjectNameAr = SubjectType.CONNECT_PLUS.titleAr,
                unitTitle = "Advanced English Notebook: Units 4, 5 & 6 (Everyday Life & My Home)",
                lessonTitle = "Present Simple (3rd Person -s/-es), Possessives, Prepositions & Digraphs th/sh/ch",
                lessonOrder = 2L,
                officialContent = """
                    English Language Lesson Preparation Notebook (Mr Sobhy Borda - Primary Two First Term - Units 4 to 6):
                    1) Unit 4 Advanced Grammar (Everyday Life & Family):
                       - Possessive Adjectives (my, your, his, her) & Possessive ('s):
                         "Who is this? — This is my father. He is tall." / "This is my grandmother. She is kind." / "This is Ahmed's car."
                       - Present Simple Tense (Subject-Verb Agreement):
                         * First/Second/Plural Subjects (I / You / We / They + base verb):
                           "I wake up at 7:00 am." / "I brush my teeth." / "They play after school."
                         * Third Person Singular (He / She / It + verb + s/es):
                           "He eats breakfast in the morning." / "She sleeps early at night." / "He washes his face." / "She brushes her hair."
                       - Time Expressions: in the morning, in the afternoon, in the evening, at night, after school.
                    2) Unit 5 & 6 Advanced Grammar (Inside My Home & Rooms at Home):
                       - Describing Locations with Prepositions of Place (in, on, under, next to):
                         Word Order: [Subject + Verb (is/are) + Preposition + Location].
                         Examples: "The bed is in the bedroom." / "The lamp is on the table." / "The ball is under the table." / "The garden is next to the house."
                       - Using "There is" vs "There are" to describe a house:
                         "There is a kitchen and a living room in my house."
                         "There are two bedrooms and four chairs in my house."
                       - Asking & Answering "Who" and "Where" Questions:
                         * "Who washes the dishes?" -> "I help my family wash the dishes."
                         * "Who cooks dinner?" -> "My mother cooks dinner in the kitchen."
                         * "Where do you sleep?" -> "I sleep in the bedroom."
                       - Complete Digraphs Mastery (th, sh, ch) & Glossary Definitions:
                         * /th/ (voiceless): thin, thick, thumb, thunder, math, teeth.
                         * /th/ (voiced): mother, father, brother, this, that.
                         * /sh/: sheep, ship, shop, fish, dish, brush.
                         * /ch/: cheese, chicken, chin, lunch, beach, chair.
                """.trimIndent(),
                keyTerms = "Third Person Singular (cooks, eats, washes, watches, sleeps, helps), Possessive 's, Prepositions (in, on, under, next to), Digraphs (th, sh, ch), Who & Where Questions",
                sourceBookName = "Mr Sobhy Borda - English Language Lesson Preparation Notebook Grade 2 (Pages 35–64) & Glossary",
                isCompleted = false
            ),

            // =========================================================================
            // 6. الكتب الإضافية الجديدة المرفوعة (المعاصر ٢٠٢٧ - ماث وإنجليزي وتقييمات شاملة)
            // =========================================================================
            "math_elmoasser_2027_ch1_to_5_sheets" to CurriculumLesson(
                id = "math_elmoasser_2027_ch1_to_5_sheets",
                userId = userId,
                subjectId = SubjectType.MATH.id,
                subjectNameAr = SubjectType.MATH.titleAr,
                unitTitle = "El-Moasser Math 2027 (Ch 1–5): Exercises & Sheets 1–13",
                lessonTitle = "كتاب المعاصر ماث ٢٠٢٧ (الفصول ١-٥): تدريبات وشيتات (Sheets 1–13)",
                lessonOrder = 5L,
                officialContent = """
                    El-Moasser Mathematics - 2nd Primary First Term 2027 (Main Book - Chapters 1 to 5, Exercises 1–13 & Sheets 1–13):
                    1) Chapter 1: Bar Graphs & Pictographs (Exercises 1–3, Sheets 1–3):
                       - Reading and interpreting data from a Bar Graph (Horizontal & Vertical):
                         * Always check the "Scale" (مقياس التدرج): counting by 1s (0, 1, 2, 3...) or counting by 2s (0, 2, 4, 6, 8, 10...) or counting by 10s (0, 10, 20, 30...).
                         * Key Comparison Questions in El-Moasser:
                           - "How many in all / altogether / total?" -> Add (+) the values.
                           - "How many more ... than ...?" or "What is the difference between ... and ...?" -> Subtract (-) the smaller number from the larger number.
                       - Pictographs (التمثيل بالصور):
                         * Always read the Key (المفتاح) first! Example: 1 Heart = 2 students, so Half a Heart = 1 student.
                    2) Chapter 2: Mental Math Strategies (Exercises 4–7, Sheets 4–7):
                       - Doubles & Near Doubles:
                         * Doubles: 6 + 6 = 12, 7 + 7 = 14, 8 + 8 = 16, 9 + 9 = 18.
                         * Doubles Plus 1: 7 + 8 = 7 + 7 + 1 = 14 + 1 = 15.
                       - Make a 10 to Add & Subtract:
                         * Addition: 8 + 5 -> break 5 into (2 + 3) -> (8 + 2) + 3 = 10 + 3 = 13.
                         * Subtraction: 15 - 7 -> break 7 into (5 + 2) -> (15 - 5) - 2 = 10 - 2 = 8.
                       - Counting On & Counting Back on the 120 Chart (+10 moves down 1 row, -10 moves up 1 row).
                       - Word Problems with Missing Addends: 9 + ? = 16 -> 16 - 9 = 7.
                    3) Chapter 3: Place Value of 3-Digit Numbers (Exercises 8–10, Sheets 8–10):
                       - Standard Form (472), Word Form (Four hundred seventy-two), Expanded Form (400 + 70 + 2).
                       - Value vs. Place Value (أهم سؤال في امتحانات المعاصر):
                         * Place Value (القيمة المكانية = اسم الخانة بالحروف): Hundreds, Tens, or Ones.
                         * Value (قيمة الرقم = بالأرقام): In 583, the Place Value of 5 is Hundreds, and the Value of 5 is 500.
                       - Smallest & Greatest 3-Digit Numbers formed from 3 digits (e.g., 4, 0, 7):
                         * Greatest: 740 | Smallest: 407 (Never put 0 in the Hundreds place!).
                    4) Chapter 4: Adding & Subtracting 2-Digit Numbers (Exercises 11–12, Sheets 11–12):
                       - Commutative Property of Addition: 24 + 15 = 15 + 24.
                       - Decomposition into Tens and Ones: 36 + 23 = (30 + 20) + (6 + 3) = 50 + 9 = 59.
                       - Addition with Regrouping (Carry over 10 ones = 1 ten): 48 + 27 = 75.
                       - Subtraction with Regrouping (Borrow 1 ten = 10 ones): 62 - 28 = 34.
                    5) Chapter 5: 2D Shapes, 3D Solids & Length Measurement (Exercise 13, Sheet 13):
                       - 2D Shapes: Triangle (3 sides, 3 vertices), Square (4 equal sides), Rectangle (2 long equal sides, 2 short equal sides), Rhombus (4 equal sides), Trapezoid (4 sides, only 2 parallel), Pentagon (5 sides), Hexagon (6 sides).
                       - 3D Solids: Cube & Cuboid/Rectangular Prism (6 faces, 12 edges, 8 vertices), Sphere (0 faces, 0 edges, 0 vertices), Cylinder (2 circular bases, 0 vertices), Cone (1 circular base, 1 vertex), Square-based Pyramid (5 faces: 1 square base + 4 triangular faces, 8 edges, 5 vertices).
                """.trimIndent(),
                keyTerms = "Scale (مقياس التدرج), Pictograph Key (مفتاح التمثيل بالصور), Place Value vs Value (القيمة المكانية مقابل قيمة الرقم), Greatest & Smallest 3-digit number, Regrouping (إعادة التجميع), Vertices/Edges/Faces",
                sourceBookName = "معاصر ماث 2ب ترم اول 2027.pdf (El-Moasser Mathematics Main Book 2027 - Chapters 1–5)",
                isCompleted = false
            ),
            "math_elmoasser_2027_ch6_to_9_glossary" to CurriculumLesson(
                id = "math_elmoasser_2027_ch6_to_9_glossary",
                userId = userId,
                subjectId = SubjectType.MATH.id,
                subjectNameAr = SubjectType.MATH.titleAr,
                unitTitle = "El-Moasser Math 2027 (Ch 6–9): Mass, Time, 3-Digit Ops & Glossary",
                lessonTitle = "كتاب المعاصر ماث ٢٠٢٧ (الفصول ٦-٩): الكتلة والوقت والقاموس الشامل (Sheets 14–26)",
                lessonOrder = 6L,
                officialContent = """
                    El-Moasser Mathematics - 2nd Primary First Term 2027 (Main Book - Chapters 6 to 9, Sheets 14–26 & Bilingual Glossary):
                    1) Chapter 6 & 7: Measuring Length, Mass (Weight) & Capacity (Exercises 14–18, Sheets 14–18):
                       - Length Units: Centimeter (cm) for small objects (pencil, key, eraser); Meter (m) for large objects (classroom door, car, tree). 1 m = 100 cm.
                       - Mass Units:
                         * Gram (gm / g): for light objects (paperclip, ring, feather, strawberry).
                         * Kilogram (kg): for heavy objects (watermelon, dog, bicycle, bag of flour).
                         * Standard Weights in El-Moasser: ½ kg (half kilogram = 500 g), 1 kg, 2 kg, 5 kg, 10 kg.
                       - Capacity (السعة): Milliliter (mL) for small liquids (medicine spoon, perfume bottle) vs. Liter (L) for large containers (water bottle, milk jug).
                    2) Chapter 8: Time (A.M. / P.M. & Reading Analog/Digital Clocks - Exercises 19–22, Sheets 19–22):
                       - 1 Day = 24 Hours (12 hours A.M. from midnight to noon + 12 hours P.M. from noon to midnight).
                       - 1 Hour = 60 Minutes | Half an hour = 30 Minutes | Quarter of an hour = 15 Minutes.
                       - Clock Hands:
                         * Minute hand on 12 -> O'clock (:00)
                         * Minute hand on 6 -> Half past (:30)
                         * Minute hand on 3 -> Quarter past (:15)
                         * Minute hand on 9 -> Quarter to (:45)
                    3) Chapter 9: Adding & Subtracting 3-Digit Numbers with Regrouping (Exercises 23–26, Sheets 23–26):
                       - Estimating Sums & Differences using Front-End Estimation (أول رقم من اليسار):
                         * 482 + 219 -> 400 + 200 = 600.
                       - Rounding to the Nearest Ten (التقريب لأقرب عشرة):
                         * Look at the Ones digit:
                           - Poor digits (0, 1, 2, 3, 4) -> keep the Tens digit as is, replace Ones with 0 (e.g., 43 -> 40).
                           - Rich digits (5, 6, 7, 8, 9) -> add 1 to the Tens digit, replace Ones with 0 (e.g., 47 -> 50, 165 -> 170).
                       - 3-Digit Addition & Subtraction with Regrouping:
                         * Example: 358 + 267 = 625 (8+7=15, write 5 carry 1 ten; 1+5+6=12 tens, write 2 carry 1 hundred; 1+3+2=6 hundreds).
                         * Example: 742 - 318 = 424 (borrow 1 ten from 4 tens -> 12 - 8 = 4 ones; 3 - 1 = 2 tens; 7 - 3 = 4 hundreds).
                    4) El-Moasser Official English-Arabic Mathematical Glossary (قاموس مصطلحات المعاصر):
                       - Ascending order (ترتيب تصاعدي من الأصغر للأكبر) | Descending order (ترتيب تنازلي من الأكبر للأصغر)
                       - Addend (المضاف) | Sum (ناتج الجمع) | Difference (الفرق / ناتج الطرح)
                       - Estimate (يقدّر) | Round (يقرّب) | Regrouping (إعادة التجميع)
                       - Polygon (مضلع مغلق ثنائي الأبعاد) | Parallel (متوازيان) | Vertex / Vertices (رأس / رؤوس) | Edge (حرف) | Face (وجه).
                """.trimIndent(),
                keyTerms = "Front-End Estimation (التقدير من خلال أول رقم من اليسار), Rounding to Nearest Ten (التقريب لأقرب عشرة), Quarter past (:15), Quarter to (:45), Ascending/Descending, Sum/Difference",
                sourceBookName = "معاصر ماث 2ب ترم اول 2027.pdf (El-Moasser Mathematics Main Book 2027 - Chapters 6–9 & Glossary)",
                isCompleted = false
            ),
            "math_elmoasser_2027_assessments_exams" to CurriculumLesson(
                id = "math_elmoasser_2027_assessments_exams",
                userId = userId,
                subjectId = SubjectType.MATH.id,
                subjectNameAr = SubjectType.MATH.titleAr,
                unitTitle = "El-Moasser Math 2027: Weekly Assessments & 15 Final Exams",
                lessonTitle = "ملحق تقييمات وامتحانات المعاصر ماث ٢٠٢٧: ١٥ تقييماً أسبوعياً و١٥ امتحاناً نهائياً",
                lessonOrder = 7L,
                officialContent = """
                    El-Moasser Assessments and Final Revision (Free Part 1) - 2nd Primary First Term 2027 (73 Pages):
                    1) First: Weekly Assessments (Weeks 1 to 15) & Chapter Assessments:
                       - Week 1 & 2 (Bar Graphs & Pictographs): Choose the correct answer + Complete using the graph + Find sum & difference.
                       - Week 3 & 4 (Doubles, Make a 10, Missing Numbers):
                         * Example 1: 8 + 8 = 16, so 8 + 9 = 17.
                         * Example 2: 14 - 6 = (14 - 4) - 2 = 10 - 2 = 8.
                         * Example 3: If 7 + a = 15, then a = 8.
                       - Week 5 & 6 (3-Digit Place Value, Comparing & Ordering):
                         * Write in standard form: 600 + 4 + 50 = 654 (Watch out for scrambled order!).
                         * Compare: 4 hundreds + 3 tens (430) [ > ] 403.
                         * Greatest 3-digit number is 999; Smallest 3-digit number is 100; Smallest 3-digit number with different digits is 102.
                       - Week 7, 8 & 9 (2-Digit Addition/Subtraction & 2D/3D Shapes):
                         * 46 + 38 = 84 | 81 - 45 = 36.
                         * Which solid has 5 faces, 8 edges, and 5 vertices? Square-based pyramid.
                         * Which solid has 2 circular faces and no vertices? Cylinder.
                       - Week 10, 11 & 12 (Length, Mass & Time):
                         * Arrange lengths from shortest to longest: 45 cm, 15 cm, 1 m (100 cm) -> 15 cm, 45 cm, 1 m.
                         * Time: When the hour hand is between 4 and 5 and the minute hand is on 9, the time is 4:45 (Quarter to 5).
                       - Week 13, 14 & 15 (3-Digit Addition, Subtraction & Rounding):
                         * Round 76 to the nearest ten -> 80 | Round 34 to the nearest ten -> 30.
                         * 527 + 246 = 773 | 650 - 238 = 412.
                    2) Second: Review Time & 15 Full Final Assessments (Exams 1 to 15):
                       - Exam Structure in El-Moasser Final Assessments:
                         * Question 1: Choose the correct answer from a, b, c, or d (6 Multiple-Choice Items covering Place Value, Time, Shapes, Operations).
                         * Question 2: Complete the following (6 Fill-in-the-blank items covering Expanded Form, Rounding, Doubles, Missing Addends, Units of Mass/Length).
                         * Question 3: Answer the following (Word Problems, Ordering Numbers Ascending/Descending, Reading a Bar Graph/Pictograph, and Drawing Clock Hands).
                       - High-Frequency Tricky Exam Questions from Assessments 1–15:
                         * Trick 1: "30 tens = ___ hundreds" -> 30 tens = 300 = 3 hundreds!
                         * Trick 2: "5 hundreds = ___ tens" -> 500 = 50 tens!
                         * Trick 3: "The number just after 399 is ___" -> 400. "The number just before 500 is ___" -> 499.
                         * Trick 4: "A polygon that has 5 sides is called a ___" -> Pentagon. "A polygon with 6 vertices is a ___" -> Hexagon.
                """.trimIndent(),
                keyTerms = "Weekly Assessments (التقييمات الأسبوعية), Final Assessments 1–15 (الامتحانات النهائية), 30 tens = 3 hundreds, Smallest 3-digit number (100), Different digits (102)",
                sourceBookName = "ملحق معاصر ماث 2ب ترم اول 2027.pdf (El-Moasser Mathematics Assessments & Final Revision 2027)",
                isCompleted = false
            ),
            "connect_elmoasser_2027_interactive_notebook" to CurriculumLesson(
                id = "connect_elmoasser_2027_interactive_notebook",
                userId = userId,
                subjectId = SubjectType.CONNECT.id,
                subjectNameAr = SubjectType.CONNECT.titleAr,
                unitTitle = "El-Moasser English 2027: A Week Is Enough & 20 Sample Exams",
                lessonTitle = "كراسة المعاصر التفاعلية إنجليزي ٢٠٢٧: المراجعة النهائية و٢٠ امتحاناً شاملاً",
                lessonOrder = 5L,
                officialContent = """
                    El-Moasser Interactive Notebook & Final Revision - 2nd Primary First Term 2027 ("A Week Is Enough - Unlock the Exam", 49 Pages):
                    1) Days 1 & 2: Revise Vocabulary ("Must-Know 20 Words") & Language Rules (Units 1 to 6):
                       - Unit 1 (Meet My Family):
                         * Vocabulary: family (عائلة), parents (الوالدان), grandparents (الأجداد), cousin (ابن/ابنة العم أو الخال), aunt (عمة/خالة), uncle (عم/خال), nurse (ممرضة), teacher (معلم), engineer (مهندس), doctor (طبيب), officer (ضابط), carpenter (نجار).
                         * Phonics (/æ/ vs /e/): dad, bag, mat, sat vs. ten, pen, net, bed.
                         * Grammar: "Who is this?" -> "This is my father." / "Who are these?" -> "These are my grandparents." / Greetings: Good morning, Good afternoon, Good evening, Good night.
                       - Unit 2 (My Body):
                         * Vocabulary: head (رأس), face (وجه), eyes (عيون), ears (أذنان), nose (أنف), mouth (فم), arm (ذراع), hand (يد), leg (ساق), foot/feet (قدم/أقدام), knee (ركبة),bird, elephant, monkey, beak, fur, wings.
                         * Grammar: "I have two eyes." / "He has one nose." / "She has two hands." / Five Senses: "I can see with my eyes.", "I can hear with my ears.", "I can smell with my nose.", "I can taste with my tongue.", "I can touch with my hands."
                       - Unit 3 (Off to School):
                         * Vocabulary: classroom, whiteboard, desk, chair, pencil case, ruler, eraser, sharpener, book, bag, crayons, sit down, stand up, open your book, close your book, listen carefully, raise your hand.
                         * Grammar: Imperatives (أسلوب الأمر) & Demonstratives (This/That for singular, These/Those for plural) & Counting 1 to 20.
                       - Unit 4 (Time to Play Sports!):
                         * Vocabulary: football, tennis, basketball, swimming, karate, running, kick, hit, throw, jump, team, club, numbers 10, 20, 30, 40, 50, 60, 70, 80, 90, 100.
                         * Grammar: Ability with Can / Can't: "Can you play tennis?" -> "Yes, I can." / "No, I can't." | Conjunctions: "I can swim, AND I can run." / "I can play football, BUT I can't play karate."
                       - Unit 5 (My Day) & Unit 6 (Healthy Lunches / My Home):
                         * Vocabulary: get up, have breakfast, go to school, go home, have lunch, do homework, watch TV, go to bed, o'clock, healthy food (fruit, vegetables, milk, fish, salad, water) vs. unhealthy food (fries, burger, soda, candy, chocolate).
                         * Grammar: Present Simple Daily Routine: "I get up at 6 o'clock." / "He gets up at 7 o'clock." / "Would you like some milk?" -> "Yes, please." / "No, thank you."
                    2) Days 3, 4, 5, 6 & 7: 20 Full Sample Tests (SB/AB Tests 1–7 & Governorate Evaluations 8–20):
                       - Question Types in El-Moasser Sample Tests (1–20):
                         * Q1 (Listen and write the missing sound / circle): Testing digraphs (sh, ch, th, ph, wh) and short/long vowels.
                         * Q2 (Choose the correct answer): Grammar & Vocabulary MCQs (e.g., "I (have - has - am) two ears", "He (get - gets - getting) up at 6 o'clock", "Can you (swim - swims - swimming)?").
                         * Q3 (Read and match A with B): Matching questions with answers.
                         * Q4 (Look, read and complete / Rearrange words to make a sentence): e.g., "is - This - father - my." -> "This is my father."
                         * Q5 (Punctuate the following sentence): Capital letter at the beginning + Full stop (.) for statements or Question mark (?) for questions (e.g., "can ali swim" -> "Can Ali swim?").
                """.trimIndent(),
                keyTerms = "Must-Know 20 Words, Pop Quizzes, Sample Tests 1–20, Have/Has, Can/Can't, This/That/These/Those, Present Simple (+s), Punctuation",
                sourceBookName = "ملحق_المعاصر_انجليزي_2ب_الترم_الاول_2027_المتميزون_1.pdf (El-Moasser English Interactive Notebook 2027)",
                isCompleted = false
            )
        )
    }

    val PYTHON_SCRIPT_PREVIEW = """
# سكربت البايثون لسحب كتب المنهج (PDF Ingestion Script)
# المسار داخل المشروع: scripts/ingest_curriculum_pdfs.py
# المتطلبات: pip install pymupdf gdown firebase-admin arabic-reshaper python-bidi

import os, re, json, fitz, gdown
import firebase_admin
from firebase_admin import credentials, firestore

# تم دمج جميع الكتب المرفوعة (٢٤ وحدة شاملة: اللغة العربية ٢٠٢٦/٢٠٢٧، التربية الدينية الإسلامية ٢٠٢٦/٢٠٢٧،
# كتاب الوزارة والمعاصر Math 2027 + التقييمات والامتحانات، وكتاب الوزارة والمعاصر English 2027 + كشكول التحضير)
# مباشرة داخل قاعدة بيانات Firebase Firestore للتطبيق بعد حذف كافة النسخ المكررة!
    """.trimIndent()
}
