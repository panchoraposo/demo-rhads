{{- define "rhads-build.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "rhads-build.labels" -}}
app.kubernetes.io/name: {{ include "rhads-build.name" . }}
app.kubernetes.io/part-of: rhads-demo
backstage.io/kubernetes-id: {{ include "rhads-build.name" . }}
{{- end }}

{{- define "image.dev-url" -}}
{{- if eq .Values.image.registry "Quay" -}}
{{- printf "%s/%s/%s" .Values.image.host .Values.image.organization .Values.image.name -}}
{{- else -}}
{{- printf "%s/%s/%s" .Values.image.host .Values.image.namespace .Values.image.name -}}
{{- end -}}
{{- end }}
