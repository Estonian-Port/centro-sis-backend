from django.shortcuts import render
import json
from rest_framework.viewsets import ModelViewSet, ReadOnlyModelViewSet
from rest_framework.permissions import IsAuthenticated
from django_filters.rest_framework import DjangoFilterBackend
from rest_framework.filters import SearchFilter, OrderingFilter
from django_filters import rest_framework as filters
from django.db.models import F, Value, BooleanField, Case, When, Q
from datetime import date
from django.views.decorators.csrf import ensure_csrf_cookie, csrf_protect
from django.views.decorators.http import require_POST
from django.contrib.auth import authenticate, login, logout
from django.middleware.csrf import get_token
from django.http import JsonResponse

from .serializers import StudentSerializer, StudentCreateSerializer, ClassOptionSerializer, ClassSerializer, EnrollmentSerializer, PaymentSerializer, PaymentListSerializer
from .querysets import student_with_finance
from .models import Class, ClassOption, Enrollment, Payment, Student

class StudentFilter(filters.FilterSet):
    is_paid  = filters.BooleanFilter(field_name='is_paid')
    is_late  = filters.BooleanFilter(field_name='is_late')
    # reuse the real column for filtering; keep alias only in the serializer
    has_family = filters.BooleanFilter(field_name='is_family_member')

    class Meta:
        model  = Student
        fields = ['active', 'DNI', 'is_paid', 'is_late', 'has_family']

class StudentViewSet(ModelViewSet):
    serializer_class = StudentSerializer
    permission_classes = []
    
    def get_queryset(self):
        return student_with_finance()
    
    def get_serializer_class(self):
        return (
            StudentCreateSerializer
            if self.action == "create"
            else StudentSerializer
        )
    
    filter_backends = [DjangoFilterBackend, SearchFilter, OrderingFilter]
    filter_class = StudentFilter
    search_fields = ['first_name', 'last_name', 'DNI']
    ordering_fields = ['last_name', 'amount_due', 'debt', 'DNI']
    ordering = ['last_name']
    
class ClassViewSet(ModelViewSet):
    queryset = Class.objects.all().order_by('name')
    serializer_class = ClassSerializer
    permission_classes = []
    
    filter_backends = [SearchFilter, OrderingFilter]
    search_fields = ['name']
    ordering_fields = ['name']
    ordering = ['name']
    
class ClassOptionViewSet(ModelViewSet):
    queryset = ClassOption.objects.select_related("klass")
    serializer_class = ClassOptionSerializer
    permission_classes = []
    
    filter_backends = [DjangoFilterBackend, SearchFilter, OrderingFilter]
    filterset_fields = ['klass', 'weekly_sessions']
    search_fields = ['klass__name']
    ordering_fields = ['klass__name', 'weekly_sessions']
    ordering = ['klass__name', 'weekly_sessions']
    
class EnrollmentViewSet(ModelViewSet):
    queryset = (
        Enrollment.objects.select_related('student', 'option')
        .order_by('start', 'student__last_name', 'student__first_name')
    )
    serializer_class = EnrollmentSerializer
    permission_classes = []
    
    filter_backends = [DjangoFilterBackend, SearchFilter, OrderingFilter]
    filterset_fields = ['student__DNI', 'option']
    search_fields = ['student__DNI', 'student__first_name', 'student__last_name', 'option__klass__name']
    ordering_fields = ['start', 'due_date', 'paid_on', 'amount_due', 'amount_paid']
    is_paid = filters.BooleanFilter(method='filter_paid')
    is_late = filters.BooleanFilter(method='filter_late')

    def filter_paid(self, qs, name, value):
        return qs.filter(is_paid=value)

class PaymentFilter(filters.FilterSet):
    def filter_late(self, qs, name, value):
        return qs.filter(is_late=value)

    class Meta:
        model  = Payment
        fields = ['method', 'enrollment__student__DNI', 'due_date']
    ordering = ['start']

class PaymentViewSet(ModelViewSet):
    permission_classes = []
    serializer_class = PaymentSerializer
    
    def get_queryset(self):
        today = date.today()
        return (
            Payment.objects
            .select_related('enrollment__student', 'enrollment__option__klass')
            .annotate(
                is_paid=Case(
                    When(amount_paid__gte=F('amount_due'), then=Value(True)),
                    default=Value(False),
                    output_field=BooleanField()
                ),
                is_late=Case(
                    When(
                        Q(amount_paid__lt=F('amount_due')) | Q(amount_paid__isnull=True),
                        due_date__lt=today,
                        then=Value(True)
                    ),
                    default=Value(False),
                    output_field=BooleanField(),
                )
        )
            .order_by('due_date')
    )
    
    filter_backends = [DjangoFilterBackend, SearchFilter, OrderingFilter]
    filterset_class = PaymentFilter
    search_fields = ['enrollment__student__first_name', 'enrollment__student__last_name', 'enrollment__student__DNI']
    ordering_fields = ['due_date', 'amount_due', 'amount_paid', 'is_paid', 'enrollment__student__DNI']
    ordering = ['due_date']

class PaymentListViewSet(ReadOnlyModelViewSet):
    serializer_class = PaymentListSerializer
    permission_classes = []         

    def get_queryset(self):
        return (
            Payment.objects
            .select_related(
                'enrollment__student',
                'enrollment__option__klass',
            )
            .order_by('-paid_on', '-id')
        )

@ensure_csrf_cookie
def csrf(request):
    return JsonResponse({"csrfToken": get_token(request)})

def me(request):
    if request.user.is_authenticated:
        return JsonResponse({'authenticated': True, 'username': request.user.username})
    return JsonResponse({'authenticated': False}, status=401)

@require_POST
@csrf_protect
def login_view(request):
    try:
        data = json.loads(request.body.decode())
    except Exception:
        return JsonResponse({'detail': 'Invalid JSON'}, status=400)

    username = data.get('username', '')
    password = data.get('password', '')
    remember = bool(data.get('remember', True))

    user = authenticate(request, username=username, password=password)
    if not user:
        return JsonResponse({'detail': 'Invalid credentials'}, status=400)

    login(request, user)

    if remember:
        request.session.set_expiry(None)
    else:
        request.session.set_expiry(0)

    return JsonResponse({'detail': 'Login successful'})

@require_POST
@csrf_protect
def logout_view(request):
    logout(request)
    return JsonResponse({'detail': 'Logout successful'})