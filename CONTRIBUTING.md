# Guía de Contribución y GitHub Flow (SmartRent)

Para mantener el orden en el equipo y evitar conflictos entre el Frontend, el Backend Core y el de Datos, utilizaremos **GitHub Flow**.

## 1. La Rama `main`
La rama `main` **siempre** debe ser estable y desplegable. **Nadie debe hacer commits directamente a `main`**.

## 2. Crear una Rama para tu Tarea (Feature Branch)
Cada vez que vayas a trabajar en una nueva tarea (ej. "Pantalla de catálogo" o "RabbitMQ"), crea una nueva rama a partir de `main` con un nombre descriptivo:
```bash
git checkout main
git pull origin main
git checkout -b feature/nombre-de-tu-tarea
```
*Ejemplos de nombres:* `feature/crud-arriendos`, `feature/kafka-audit`, `fix/navbar-login`.

## 3. Haz tus Commits
Trabaja en tu código y haz commits pequeños y descriptivos:
```bash
git add .
git commit -m "Añadido controlador de arriendos en ms-smartrent-rentals"
```

## 4. Sube tus cambios y abre un Pull Request (PR)
Cuando termines, sube tu rama a GitHub:
```bash
git push origin feature/nombre-de-tu-tarea
```
Ve a GitHub y abre un **Pull Request (PR)** hacia la rama `main`. El PR debe explicar qué hiciste y debe ser revisado por al menos **uno de tus compañeros** antes de hacer *Merge*.

## 5. Revisión y Merge
Una vez aprobado el PR, el código se integra a `main`. Luego, puedes borrar tu rama local y volver al Paso 2 para tu siguiente tarea.
