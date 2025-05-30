
# 📦 Distributed File System - Java 8 Compatible Version

نظام موزع بسيط باستخدام Java RMI ويدعم تسجيل الدخول، إضافة وتعديل وحذف وقراءة الملفات، والمزامنة بين العقد باستخدام Socket.

## ✅ الميزات:
- متوافق مع Java 1.8 و Java 11
- يدعم:
  - تسجيل دخول وتوليد توكن
  - تنفيذ عمليات الملفات: قراءة، كتابة، حذف
  - مزامنة الملفات بين العقد باستخدام Sockets

---

## 🔧 متطلبات التشغيل:
- Java JDK 1.8 أو 11
- تشغيل RMI Registry يدويًا
- Terminal أو IntelliJ IDEA

---

## ⚙️ خطوات التشغيل:

### 1️⃣ Compile:
```bash
javac */*.java
```

### 2️⃣ شغل RMI Registry:
```bash
rmiregistry 1099
```

### 3️⃣ شغل Node:
```bash
java node.NodeServer
```

### 4️⃣ شغل Coordinator:
```bash
java coordinator.Coordinator
```

### 5️⃣ شغل العميل:
```bash
java client.ClientApp
```

### 6️⃣ تجربة العمليات:
- اسم المستخدم: qa1
- كلمة المرور: qa
- ملف: `report.txt`
- كتابة: محتوى عادي
- حذف: yes

---

## 🔁 مزامنة باستخدام Socket:

### شغّل FileSyncServer على الطرف المستقبل:
```bash
java sync.FileSyncServer
```

### من الطرف المرسل:
```bash
java sync.FileSyncClient 127.0.0.1 9000 files/report.txt
```
javac node/*.java coordinator/*.java client/*.ja
va shared/*.java sync/*.java

