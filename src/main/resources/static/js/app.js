var projectBoosterTpl = "<div id=\"projectBooster\" class=\"form-group col-sm-6\"><div class=\"checkbox\"><label> " +
    "<input type=\"checkbox\" id=\"{{id}}\" name=\"projectBooster\" value=\"{{id}}\">" +
    "&nbsp;&nbsp;<strong>{{name}}</strong>" +
    "<p class=\"help-block\">{{#adoc}} {{description}} {{/adoc}}</p></label></div></div>";

var asciiDoctor = Asciidoctor();

function docToHTML(txtAdoc) {
    var html = asciiDoctor.converter(txtAdoc);
    console.log(html);
    return html;
}

$(function () {

    var ws = null;

    var wsConnect = function () {
        ws = new WebSocket('ws://localhost:8080/boosters');
        ws.onmessage = function (event) {
            //console.log("Data"+event)
            //console.log("Data Body"+event.data)
            var boosterJson = JSON.parse(event.data);
            displayBooster(boosterJson);
        };

        ws.onopen = function (event) {
            ws.send("");
        };

    };

    var wsDisconnect = function () {
        if (ws != null) {
            ws.close();
        }
        console.log("Disconnected");
    };

    var displayBooster = function (boosterJson) {
        if (boosterJson) {
            $('#noCatalog').hide();
            var html = Mustache.to_html(projectBoosterTpl, boosterJson);
            //console.log(html);
            $('#catalogs').append(html);
        }
    };

    wsConnect();

    var addTag = function (id, name) {
        if ($("#depsTags div[data-id='" + id + "']").length == 0) {
            $("#depsTags").append("<div class='tag' data-id='" + id + "'>" + name +
                "<button type='button' class='close' aria-label='Close'><span aria-hidden='true'>&times;</span></button></div>");
        }
    };

    var removeTag = function (id) {
        $("#depsTags div[data-id='" + id + "']").remove();
    };

    var depsSearcher = function (engine, bootVersion) {

        $.getJSON({
            url: '/ui/dependencies.json?version=' + bootVersion
        }).done(function (data) {
            engine.clear();
            $.each(data.dependencies, function (i, item) {
                if (item.weight === undefined) {
                    item.weight = 0;
                }
            });
            engine.add(data.dependencies);
        })
    };

    var bDeps = new Bloodhound({
        datumTokenizer: Bloodhound.tokenizers.obj.nonword('name', 'description', 'keywords', 'group'),
        queryTokenizer: Bloodhound.tokenizers.nonword,
        identify: function (item) {
            return item.id;
        },
        sorter: function (left, right) {
            return right.weight - left.weight;
        },
        cache: false,
        limit: 5
    });

    depsSearcher(bDeps, $('#bootVersion').val());

    $("#bootVersion").on("change", function (e) {
        depsSearcher(bDeps, this.value);
    });

    $('#depsList').typeahead({
        minLength: 2,
        autoSelect: true,
    }, {
        name: 'bDeps',
        display: 'name',
        source: bDeps,
        templates: {
            suggestion: function (dep) {
                return "<div><strong>" + dep.name + "</strong>&nbsp;&nbsp;<small>" + dep.description + "</small></div>"
            }
        }
    });


    $('#depsList').bind('typeahead:select', function (event, dep) {
        var chkBox = $("#depsAdvDetails input[value='" + dep.id + "']");
        var isSelected = chkBox.prop('checked');
        if (isSelected) {
            removeTag(id);
            chkBox.prop('checked', false);
        } else {
            addTag(dep.id, dep.name);
            chkBox.prop('checked', true);
        }
        $('#depsList').typeahead('val', '');
    });

    $('#depsTags').on('click', "button", function () {
        var id = $(this).parent().attr('data-id');
        removeTag(id);
        $("#depsAdvDetails input[value='" + id + "']").prop('checked', false);
    })

    $("#depsAdvDetails input").bind("change", function () {
        var value = $(this).val()
        if ($(this).prop('checked')) {
            var results = bDeps.get(value);
            addTag(results[0].id, results[0].name);
        } else {
            removeTag(value);
        }
    });

    $("#projectMissions input").bind("change", function () {
        $('#projectMissions input').not(this).prop('checked', false);
    });


    //Accordion

    // Add minus icon for collapse element which is open by default
    $(".collapse.in").each(function () {
        $(this).siblings(".panel-heading").find(".glyphicon").addClass("glyphicon-minus").removeClass("glyphicon-plus");
    });

    // Toggle plus minus icon on show hide of collapse element
    $(".collapse").on('show.bs.collapse', function () {
        $(this).parent().find(".glyphicon").removeClass("glyphicon-plus").addClass("glyphicon-minus");
    }).on('hide.bs.collapse', function () {
        $(this).parent().find(".glyphicon").removeClass("glyphicon-minus").addClass("glyphicon-plus");
    });

});
