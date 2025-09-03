from django.contrib import admin
from django.urls import include, path
from core.views import csrf, StudentViewSet, ClassViewSet, ClassOptionViewSet, EnrollmentViewSet, PaymentViewSet, PaymentListViewSet, me, login_view, logout_view
from rest_framework.routers import DefaultRouter
from rest_framework.authtoken.views import obtain_auth_token

router = DefaultRouter()
router.register(r"students", StudentViewSet, basename="students")
router.register(r"classes", ClassViewSet, basename="classes")
router.register(r"class-options", ClassOptionViewSet, basename="class-options")
router.register(r"enrollments", EnrollmentViewSet, basename="enrollments")
router.register(r"payments", PaymentViewSet, basename="payments")
router.register(r"payments-simple", PaymentListViewSet, basename="payments-simple",)

urlpatterns = [
    path('nested_admin/', include("nested_admin.urls")),
    path('admin/', admin.site.urls),
    path('api/csrf/', csrf),
    path("api/csrf/", csrf.as_view(), name="csrf"),
    path("api/auth/me", me, name="auth-me"),  # no slash
    path("api/auth/me/", me, name="auth-me-slash"),
    path("api/me/", me, name="me-compat"),  # optional, for current frontend
    path("api/auth/login/", login_view.as_view(), name="login"),
    path("api/auth/logout/", logout_view.as_view(), name="logout"),
    path('api/', include(router.urls)),
    path('api/auth/', obtain_auth_token),
]
