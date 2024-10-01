# In progress

# Домашнее задание к уроку Android Lint
## Задание #1  
  
Корутины, запущенные на `kotlinx.coroutines.GlobalScope` нужно контролировать вне скоупа класс, в котором они созданы. Контролировать глобальные корутины неудобно, а отсутствие контроля может привести к излишнему использованию ресурсов и утечкам памяти.  
  
Подробнее про `GlobalScope`: [https://elizarov.medium.com/the-reason-to-avoid-globalscope-835337445abc](https://elizarov.medium.com/the-reason-to-avoid-globalscope-835337445abc)  
  
### Что нужно сделать  
  
1. Реализуйте детектор, который найдет использования `GlobalScope` в коде приложения. В качестве значения поля `id` у вашего инстанса `Issue` обязательно укажите **GlobalScopeUsage** (это важно при проверки ДЗ)  
2. Если `kotlinx.coroutines.GlobalScope` используется в классе-наследнике `androidx.lifecycle.ViewModel`, в качестве LintFix’a предложите заменить `GlobalScope` на `viewModelScope`  
   
    💡 Проверьте что подключен артефакт `androidx.lifecycle:lifecycle-viewmodel-ktx`, в котором лежит экстеншен функция `viewModelScope`. Если его нет в classpath, не предлагайте этот фикс  
 
  
3. Аналогично, если `GlobalScope` используется в наследниках `androidx.fragment.app.Fragment`, предложите заменить использование `GlobalScope` на `lifecycleScope` (`androidx.lifecycle:lifecycle-runtime-ktx`**)**  
4. Напишите тесты на ваш детектор. В качестве фрагментов кода с ошибкой используйте функции в классе `GlobalScopeTestCase`  
  
## Задание #2  
  
Частая ошибка при использовании корутин - передача экземпляра `Job`/`SupervisorJob` в корутин билдер. Хоть `Job` и его наследники являются элементами `CoroutineContext`, их использование внутри корутин-билдеров не имеет никакого эффекта, это может сломать ожидаемые обработку ошибок и механизм отмены корутин.  
  
Подробнее:  
* [https://medium.com/androiddevelopers/exceptions-in-coroutines-ce8da1ec060c](https://medium.com/androiddevelopers/exceptions-in-coroutines-ce8da1ec060c)  
* [https://www.lukaslechner.com/7-common-mistakes-you-might-be-making-when-using-kotlin-coroutines/](https://www.lukaslechner.com/7-common-mistakes-you-might-be-making-when-using-kotlin-coroutines/)  
  
Использование еще одного наследника `Job` - `NonCancellable` внутри корутин-билдеров сломает обработку ошибок у всех корутин в иерархии  
  
Подробнее про `NonCancellable`: ****[https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-non-cancellable/index.html](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-non-cancellable/index.html)  
  
### Что нужно сделать  
  
1. Реализуйте детектор, который найдет использования экземпляров `kotlinx.coroutines.Job` внутри корутин билдеров `async` и `launch`.  
  
   В качестве значения поля `id` у вашего инстанса `Issue` обязательно укажите **JobInBuilderUsage** (это важно при проверки ДЗ)  
  
2. Реализуйте следующие LintFix’ы:  
   - Если внутри корутин-билдера используется экземляр SupervisorJob, и корутин билдер вызывается на viewmodelScope внутри наследника ViewModel, то в качестве LintFix просто удалите SupervisorJob  
   - Если внутри корутин-билдера используется экземляр `kotlinx.coroutines.NonCancellable` и корутин-билдер создает дочернюю корутину, предложите заменить вызов `launch`/`async` на `withContext`  
   - Если внутри корутин билдер используется любой другой экземпляр `Job`, либо `SupervisorJob`, но класс не наследуется от `androidx.lifecycle.ViewModel`, то просто напишите о проблеме в репорте, не предлагая никаких LintFix’ов.  
  
  💡 Проверьте что подключен артефакт `androidx.lifecycle:lifecycle-viewmodel-ktx`, в котором лежит экстеншен функция `viewModelScope`. Если его нет в classpath, не предлагайте этот фикс  
  
  
3. Напишите тесты на ваш детектор. В качестве фрагментов кода с ошибкой используйте функции в классе `JobInBuilderTestCase`  
  
## Задание #3  
  
В основах любой дизайн системы лежат атомы: цвета, шрифты, иконки и т.п. Чтобы получить максимальную пользу от дизайн системы, использование цветов должно быть ограничено установленной палитрой. В этом задании ограничимся базовой палитрой из нескольких цветов.  
  
### Что нужно сделать  
  
1. Напишите детектор, который проверяет что все цвета, которые используются в ресурсах приложения находятся в палитре. За палитру будем считать цвета описанные в файлер `colors.xml`  
  
   В качестве значения поля `id` у вашего инстанса `Issue` обязательно укажите **JobInBuilderUsage** (это важно при проверки ДЗ)  
  
	💡 В этом задании нужно запускать анализ повторно, либо дожидаться завершения анализа всех проектов(и всех групп файлов) и проверять соответствие использованных цветов к допустимым цветам в коллбеке `afterCheckProject`  


1. Если используемый цвет совпадает с одним из цветов в палитре(например в одном из виджетов использован raw цвет `android:background="#FF000000"`, а в палитре есть аналогичный ему `<color name="black">#FF000000</color>`, то в качестве LintFix предложите заменить raw цвет на цвет из палитры  
  
   В остальных случаях репортите инцидент, пользователь сам решит нужно ли добавлять новый цвет в палитру, либо выбрать один из доступных вариантов  
  
2. Обратите внимание что ссылки на цвет возможны не только в layout ресурсах. Продумывать все аттрибуты где можно встретиться цвет не нужно, но проработайте все кейсы которые встречаются в приложении  
3. Напишите тесты на ваш детектор. В качестве фрагментов кода с ошибкой используйте ресурсы в файлах *incorrect_color_usages_layout.xml*,  *selector.xml* и *ic_baseline_adb_24.xml*
