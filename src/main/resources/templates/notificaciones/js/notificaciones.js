// ../notificaciones/js/notificaciones.js

$(document).ready(function(){
    // Crear el modal de notificaciones dinámicamente si no existe
    if (!document.getElementById('modalNotificaciones')) {
        $('body').append(`
            <div class="modal fade" id="modalNotificaciones" tabindex="-1" role="dialog" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered" role="document">
                    <div class="modal-content shadow-lg">
                        <div class="modal-header bg-info text-white">
                            <h5 class="modal-title"><i class="fas fa-bell mr-2"></i>Notificaciones</h5>
                            <button type="button" class="close text-white" data-dismiss="modal"><span>&times;</span></button>
                        </div>
                        <div class="modal-body" id="modalNotificacionesCuerpo"></div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-dismiss="modal">Cerrar</button>
                        </div>
                    </div>
                </div>
            </div>
        `);
    }

    $.get("/notifications/notifications", function(data){
        var notifications = data;
        var cuerpo;
        if(notifications.length > 0) {
            var items = notifications.map(function(n){ return '<li class="py-1">' + n.mensaje + '</li>'; }).join('');
            cuerpo = '<ul class="list-unstyled mb-0">' + items + '</ul>';
        } else {
            cuerpo = '<p class="text-muted text-center mb-0"><i class="fas fa-check-circle text-success mr-2"></i>No tienes nuevas notificaciones.</p>';
        }
        document.getElementById('modalNotificacionesCuerpo').innerHTML = cuerpo;
        $('#modalNotificaciones').modal('show');
    });
});
