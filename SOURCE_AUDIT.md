# سجل تدقيق المصادر

تاريخ مراجعة هذه المرحلة: 14 أكتوبر 2026. هذا سجل فني لمصادر الشيفرة، وليس رأيًا قانونيًا. تحقق من ملف LICENSE وNOTICE في الالتزام المحدد قبل أي دمج إضافي أو توزيع ثنائي.

| المشروع | الرابط | نتيجة التحقق/الترخيص | ما استخدمه المشروع |
|---|---|---|---|
| Kototoro | https://github.com/Kototoro-app/Kototoro | Apache-2.0؛ التزام `6410cdd1e1a92031f429c46cec39b5d2ea041fa2`، فرع `devel` | نُقل `reader-core` فقط، لا التطبيق أو واجهته الكاملة |
| Grimoire | https://github.com/Operation-Grimoire/grimoire | Apache-2.0؛ التزام `8016041da36d8db3a6935268aa144d798cf204c0`، المشروع يصف نفسه Alpha | نُقل `EpubParser.kt` فقط؛ لم يُنقل نظام إضافاته أو قاعدة بياناته |
| Kotori | https://github.com/tailolicon/kotori | Apache-2.0 وفق LICENSE وREADME اللذين تمت مراجعتهما | لم تُنسخ شيفرة؛ لا تُستخدم هويته أو علامته |
| Nekoyomi | https://github.com/Seguaz/Nekoyomi | Apache-2.0 وفق LICENSE؛ تطبيق مبني على Aniyomi، مع تبعيات مستقلة تحتاج فحصًا | لم تُنسخ شيفرة أو ملحقات |
| Mihon | https://github.com/mihonapp/mihon | Apache-2.0 وفق LICENSE | لم تُنسخ شيفرة أو امتدادات |
| Aniyomi | https://github.com/aniyomiorg/aniyomi | Apache-2.0 وفق LICENSE | لم تُنسخ شيفرة أو امتدادات |
| Media3 | https://github.com/androidx/media | Apache-2.0؛ الاعتماد المباشر `media3-exoplayer` و`media3-ui` بإصدار `1.9.3` | تشغيل ملف فيديو محلي فقط؛ لا بث أو خيار جودة/صوت/ترجمة مصطنع |
| CloudStream | https://github.com/recloudstream/cloudstream | جرى فحصه كمصدر مرشح؛ لم تُنقل شيفرة أو إضافات | لا محرك فيديو أو إضافة مفعلة |
| OpenStream | https://github.com/Ivorisnoob/OpenStream | جرى فحص البنية المرشحة؛ لم تُنقل كتالوجات أو حمولة مصادر | لا نظام كتالوج JSON ضمن التطبيق |
| Material Files | https://github.com/zhanghai/MaterialFiles | جرى فحصه؛ لم تُنسخ ملفات مصدر منه | مديرنا يستخدم Android SAF، ولا يعتمد على مشروع Material Files |
| mpv-android | https://github.com/mpv-android/mpv-android | جرى فحصه كمشغل مرشح؛ لم تُنقل شيفرته أو مكتباته الأصلية | غير مدمج؛ لا حزمة native أو إعدادات وهمية |
| Zangetsu | https://github.com/Spyou/Zangetsu | استُبعد وفق توجيه المستخدم لأنه Flutter | لا شيفرة أو أصول مستخدمة |
| Club Stream | غير محدد | لا يمكن التحقق من المستودع المقصود؛ لم يُخمن رابطًا بديلًا | لا شيء مستخدم |

## قيود التراخيص

ترخيص المستودع الجذري لا يغطي تلقائيًا كل مكتبة أو أصل أو إضافة. احتُفظ بترخيص Kototoro في `LICENSE` وبترخيص Grimoire في `licenses/Grimoire-LICENSE` مع namespaces والإشعارات. يجب إبقاء نصوص Apache-2.0 وإشعارات حقوق المؤلف، وتحديد الملفات المعدلة إذا عُدلت ملفات المصدر المنقولة مستقبلًا. هوية Frankenstein Lab مستقلة ولا تدّعي اعتمادًا من المشاريع الأصلية.

الاعتماد المباشر على Jsoup `1.17.2` موثق من وصفة Grimoire API، ويجب تمرير إشعار MIT المناسب عند التوزيع. AndroidX وMedia3 وCoroutines تُذكر تراخيصها المنشورة كمشاريع Apache-2.0؛ يلزم تأكيد أرقام النسخ والملفات الدقيقة من حل Gradle الفعلي. لم تُحل التبعيات في هذه المرحلة، ولذلك لا يوجد SBOM نهائي أو قائمة تراخيص انتقالية مكتملة.

## مصادر الأدلة

ملفات LICENSE/README الفعلية وروابط GitHub أعلاه هي الأدلة الأساسية. التقارير التفصيلية التي سجلت المصدر وقيود الترخيص خلال الفحص محفوظة في سجل تدقيق المهمة؛ يجب التحقق من ملفات upstream مجددًا عند تحديث أي التزام. لا يعني هذا السجل مراجعة قانونية أو ضمانًا لاستمرار المستودعات أو قانونية أي محتوى خارجي.
